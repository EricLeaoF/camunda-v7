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
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.ByteString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtractData implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) execution.getVariable("file");
        System.out.println("Arquivo recebido (ByteArrayInputStream): " + byteArrayInputStream);

        byte[] decodedBytes = byteArrayInputStream.readAllBytes();
        ByteString imgBytes = ByteString.copyFrom(decodedBytes);

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            List<AnnotateImageRequest> requests = new ArrayList<>();
            Image img = Image.newBuilder().setContent(imgBytes).build();

            // Request for Label Detection
            Feature labelDetection = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
            AnnotateImageRequest labelRequest = AnnotateImageRequest.newBuilder()
                    .addFeatures(labelDetection)
                    .setImage(img)
                    .build();
            requests.add(labelRequest);

            // Request for Text Detection
            Feature textDetection = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
            AnnotateImageRequest textRequest = AnnotateImageRequest.newBuilder()
                    .addFeatures(textDetection)
                    .setImage(img)
                    .build();
            requests.add(textRequest);

            // Send batch request
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
            AnnotateImageResponse labelResult = response.getResponsesList().get(0); // Label detection result
            AnnotateImageResponse textResult = response.getResponsesList().get(1);  // Text detection result

            boolean isIdentityDocument = false;

            if (labelResult.hasError()) {
                System.out.format("Erro no label detection: %s%n", labelResult.getError().getMessage());
            }

            if (textResult.hasError()) {
                System.out.format("Erro no text detection: %s%n", textResult.getError().getMessage());
            }
                           
            TextAnnotation annotations = textResult.getFullTextAnnotation();
            System.out.println(annotations.getText());

            // Process label detection
            for (EntityAnnotation annotation : labelResult.getLabelAnnotationsList()) {
                String description = annotation.getDescription().toLowerCase();
                System.out.format("Descrição detectada: %s (pontuação: %f)%n", description, annotation.getScore());

                if (description.contains("identity document")) {
                    isIdentityDocument = true;
                    break;
                }
            }

            if (isIdentityDocument) {
                System.out.println("Documento de identidade identificado na imagem.");
                execution.setVariable("identity_document", true);
            } else {
                System.out.println("Documento não identificado como identidade.");
                execution.setVariable("identity_document", false);
            }
            
            execution.setVariable("data", annotations.getText());

        } catch (IOException e) {
            System.out.println("Erro ao processar a imagem: " + e.getMessage());
        }
    }
}
