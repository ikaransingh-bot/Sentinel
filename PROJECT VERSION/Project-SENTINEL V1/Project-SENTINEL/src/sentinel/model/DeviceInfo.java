package sentinel.model;

/**
 * Device dat aan de employee is toegewezen.
 */
public class DeviceInfo {

    private final int deviceId;
    private final String deviceName;
    private final String deviceType;
    private final String brand;
    private final String model;
    private final String operatingSystem;
    private final String serialNumber;
    private final String status;

    public DeviceInfo(int deviceId, String deviceName, String deviceType, String brand, String model,
                      String operatingSystem, String serialNumber, String status) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.brand = brand;
        this.model = model;
        this.operatingSystem = operatingSystem;
        this.serialNumber = serialNumber;
        this.status = status;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getStatus() {
        return status;
    }

    /** Bijvoorbeeld: Laptop-01 [LAPTOP] Dell Latitude - ACTIVE */
    public String getDisplayName() {

        StringBuilder value = new StringBuilder();
        value.append(deviceName);

        if (deviceType != null && !deviceType.isBlank()) {
            value.append(" [").append(deviceType).append("]");
        }

        if (brand != null && !brand.isBlank()) {
            value.append(" ").append(brand);
        }

        if (model != null && !model.isBlank()) {
            value.append(" ").append(model);
        }

        value.append(" - ").append(status);

        return value.toString();
    }
}
