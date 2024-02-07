package smarthome;

/**
 * Eine smarte Lampe. Enthält als Eigenschaften einen Namen und
 * einen Stromverbrauch (in Watt). Der Zustand einer Lampe
 * besteht aus einem boolean für ein/aus und einer Helligkeits-
 * einstellung. Die Helligkeit kann unabhängig vom ein/aus-Zustand
 * verändert werden.
 */
public class Lamp {
    private String name;
    private double powerConsumption; // Watt
    private boolean on = true;
    private double brightness = 1.0;

    public Lamp(String name, double powerConsumption) {
        this.name = name;
        this.powerConsumption = powerConsumption;
    }

    public String getName() {
        return name;
    }

    public double getPowerConsumption() {
        return powerConsumption;
    }

    public boolean isOn() {
        return on;
    }

    public void turnOn() {
        on = true;
    }

    public void turnOff() {
        on = false;
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = clamp(brightness);
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        } else if (value > 1.0) {
            return 1.0;
        } else {
            return value;
        }
    }
}
