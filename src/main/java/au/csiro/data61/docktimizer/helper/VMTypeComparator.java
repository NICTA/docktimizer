package au.csiro.data61.docktimizer.helper;

import au.csiro.data61.docktimizer.models.VMType;

/**
 */
public class VMTypeComparator implements java.util.Comparator<VMType> {

    @Override
    public int compare(VMType o1, VMType o2) {
        if (o1.cores < o2.cores) {
            return -1;
        }
        if (o1.cores > o2.cores) {
            return 1;
        } else return 0;
    }
}
