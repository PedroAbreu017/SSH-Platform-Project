// src/main/java/com/platform/model/enums/ContainerStatus.java
package com.platform.model.enums;

public enum ContainerStatus {
    CREATED("created"),
    RUNNING("running"),
    PAUSED("paused"),
    RESTARTING("restarting"),
    REMOVING("removing"),
    EXITED("exited"),
    DEAD("dead");

    private final String dockerStatus;

    ContainerStatus(String dockerStatus) {
        this.dockerStatus = dockerStatus;
    }

    public String getDockerStatus() {
        return dockerStatus;
    }

    public static ContainerStatus fromDockerStatus(String dockerStatus) {
        for (ContainerStatus status : ContainerStatus.values()) {
            if (status.dockerStatus.equalsIgnoreCase(dockerStatus)) {
                return status;
            }
        }
        return EXITED; // Default fallback
    }

    public boolean isActive() {
        return this == RUNNING || this == RESTARTING;
    }

    public boolean canConnect() {
        return this == RUNNING;
    }
}
