package tn.esprit.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.util.Map;

public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final boolean configured;

    public CloudinaryService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String cloudName = dotenv.get("CLOUDINARY_CLOUD_NAME");
        String apiKey = dotenv.get("CLOUDINARY_API_KEY");
        String apiSecret = dotenv.get("CLOUDINARY_API_SECRET");
        this.configured = hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);

        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public String uploadImage(File file) {
        if (!configured) {
            throw new IllegalStateException("Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET.");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
