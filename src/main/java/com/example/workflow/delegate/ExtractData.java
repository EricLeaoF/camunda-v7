package com.example.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtractData implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        
        // Recebe o ByteArrayInputStream da variável 'file' no DelegateExecution
        ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) execution.getVariable("file");
        System.out.println("Arquivo recebido (ByteArrayInputStream): " + byteArrayInputStream);

        // Converte o conteúdo do ByteArrayInputStream em um array de bytes
        byte[] decodedBytes = byteArrayInputStream.readAllBytes();
        ByteString imgBytes = ByteString.copyFrom(decodedBytes);

        // Inicializa o cliente da Google Vision API
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            // Cria o pedido de anotação de imagem
            List<AnnotateImageRequest> requests = new ArrayList<>();
            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
            AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
            requests.add(request);

            // Realiza a detecção de rótulos na imagem
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            // Processa a resposta da API
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Erro: %s%n", res.getError().getMessage());
                    return;
                }

                for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
                    annotation
                        .getAllFields()
                        .forEach((k, v) -> System.out.format("%s : %s%n", k, v.toString()));
                }
            }
        }
    }
}
