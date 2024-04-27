class Demo3 {
    static String getSecret() {
        // data received from the internet
        return "6666666";
    }

    static String sanitizeSecret(String data) {
        // Simple sanitizer that cuts the string to the first 3 characters
        return data.substring(0, Math.min(3, data.length()));
    }

    public static void main(String[] args) {
        String secret = getSecret();
        String sanitizedSecret = sanitizeSecret(secret);  // Sanitize the secret
        String publicInfo = "This is public information.";

        int secretLength = sanitizedSecret.length();  // Use sanitized data
        String message = "";

        for (int i = 0; i < secretLength; i++) {
            message += "ou la";
        }

        System.out.println("Jo ta ro say: " + message);
    }
}
