package au.csiro.data61.docktimizer.models;

/**
 */
public enum DockerConfiguration {

    MICRO_CORE(0.5, 50, 30),
    SINGLE_CORE(1, 100, 30),
    DUAL_CORE(2, 2 * 100, 30),
    QUAD_CORE(4, 4 * 100, 30),
    HEXA_CORE(8, 8 * 100, 30);

    public final String id;
    public final double cores; //amount of needed VCores
    public final double ram; //amount of needed memory in mb
    public final double disc; //amount of needed disc space in mb

    DockerConfiguration(double cores, double ram, double disc) {
        this.id = "c" + String.valueOf(cores);
        this.cores = cores;
        this.ram = ram;
        this.disc = disc;
    }
}
