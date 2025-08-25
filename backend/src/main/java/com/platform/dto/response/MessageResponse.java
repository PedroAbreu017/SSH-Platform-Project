package com.platform.dto.response;

public class MessageResponse {
    private String message;
    private String type = "info"; // info, success, warning, error

    public MessageResponse() {}

    public MessageResponse(String message) {
        this.message = message;
    }

    public MessageResponse(String message, String type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // Utility factory methods
    public static MessageResponse success(String message) {
        return new MessageResponse(message, "success");
    }

    public static MessageResponse error(String message) {
        return new MessageResponse(message, "error");
    }

    public static MessageResponse info(String message) {
        return new MessageResponse(message, "info");
    }

    public static MessageResponse warning(String message) {
        return new MessageResponse(message, "warning");
    }
}
