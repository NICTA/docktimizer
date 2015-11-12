package au.csiro.data61.docktimizer.models;

/**
 * TODO: add more VM types according AWS, these here represent the OpenStack VMs
 */
public enum VMType {

    M1_MICRO(1, 960, 40, 10, "m1.micro", "t2.micro"),
    T2_MICRO(1, 960, 40, 10, "t2.micro", "t2.micro"),
    M1_SMALL(1, 1820, 40, 10, "m1.small", "t2.small"),
    M1_MEDIUM(2, 3.750, 40, 20 * 0.9, "m1.medium", "t2.medium"),
    M1_LARGE(4, 7.680, 40, 40 * 0.8, "m1.large","c4.xlarge");


    public final String id;
    public final double price; //price in cost units
    public final int cores; //amount of VCores, can be half aswell
    public final double ram; //memory in mb
    public final double disc; //disc space in mb
    public final long leasingDuration = 5 * 60 * 1000; //leasing duration in milliseconds
    public final long startupTime = 90 * 1000; //startup a vm takes 90 seconds
    public final String flavorName;
    public final String awsFlavor;


    VMType(int cores, double ram, double disc, double price, String flavorName, String awsFlavor) {
        this.id = "v" + String.valueOf(cores);
        this.cores = cores;
        this.ram = ram;
        this.disc = disc;
        this.price = price;
        this.flavorName = flavorName;
        this.awsFlavor = awsFlavor;
    }

    public static VMType getByFlavorName(String flavorName) {
        switch (flavorName) {
            case "m1.small":
                return M1_SMALL;
            case "m1.micro":
                return M1_MICRO;
            case "m1.medium":
                return M1_MEDIUM;
            case "m1.large":
                return M1_LARGE;
            case "t2.micro":
                return T2_MICRO;
        }
        return M1_MICRO;
    }

    public String getReadableString() {
        switch ((int) cores) {
            case 2:
                return "DUAL_CORE_MEDIUM";
            case 4:
                return "QUAD_CORE_LARGE";
            case 8:
                return "HEXA_CORE_XLARGE";
            default:
                if (this.equals(M1_MICRO))
                    return "SINGLE_CORE_MICRO";
                else return "SINGLE_CORE_SMALL";
        }
    }

}
