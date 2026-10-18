package com.example.wardrobeiq.utils;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;

public class ClothingImageAnalyzer {

    public interface AnalysisCallback {

        void onWardrobeItemDetected(
                List<ImageLabel> labels
        );

        void onNotWardrobeItem();

        void onError(Exception exception);
    }

    private final ImageLabeler imageLabeler;

    public ClothingImageAnalyzer() {

        ImageLabelerOptions options =
                new ImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.50f)
                        .build();

        imageLabeler =
                ImageLabeling.getClient(options);
    }

    public void analyze(
            Bitmap bitmap,
            AnalysisCallback callback) {

        if (bitmap == null) {

            callback.onError(
                    new IllegalArgumentException(
                            "Image is empty"
                    )
            );

            return;
        }

        InputImage image =
                InputImage.fromBitmap(
                        bitmap,
                        0
                );

        imageLabeler.process(image)
                .addOnSuccessListener(
                        labels -> {

                            if (isWardrobeItem(labels)) {

                                callback
                                        .onWardrobeItemDetected(
                                                labels
                                        );

                            } else {

                                callback
                                        .onNotWardrobeItem();
                            }
                        }
                )
                .addOnFailureListener(
                        exception ->
                                callback.onError(
                                        exception
                                )
                );
    }

    private boolean isWardrobeItem(
            List<ImageLabel> labels) {

        for (ImageLabel label : labels) {

            String text =
                    label.getText()
                            .toLowerCase();

            float confidence =
                    label.getConfidence();

            if (confidence < 0.50f) {
                continue;
            }

            if (isClothingLabel(text)) {
                return true;
            }
        }

        return false;
    }

    private boolean isClothingLabel(
            String label) {

        return label.contains("clothing")
                || label.contains("clothes")
                || label.contains("shirt")
                || label.contains("t-shirt")
                || label.contains("jeans")
                || label.contains("trouser")
                || label.contains("pants")
                || label.contains("dress")
                || label.contains("skirt")
                || label.contains("shorts")
                || label.contains("jacket")
                || label.contains("coat")
                || label.contains("sweater")
                || label.contains("hoodie")
                || label.contains("shoe")
                || label.contains("footwear")
                || label.contains("sandal")
                || label.contains("sneaker")
                || label.contains("boot")
                || label.contains("hat")
                || label.contains("cap")
                || label.contains("scarf")
                || label.contains("bag")
                || label.contains("handbag")
                || label.contains("accessory");
    }
}