package edu.umn.amicus.distillers;

import edu.umn.amicus.AlignedTuple;
import edu.umn.amicus.PreAnnotation;

/**
 * Distiller that will take the first Annotation it comes across and ignore the others.
 * Be sure to list Types in order of priority (highest first) in the configuration.
 *
 * Created by gpfinley on 10/20/16.
 */
public class PriorityDistiller implements Distiller<Object> {

    /**
     *
     * Contract: each annotation is a different type, in the same order listed in configuration.
     *              Higher-priority annotations listed first.
     * @param annotations
     */
    @Override
    public PreAnnotation<Object> distill(AlignedTuple<PreAnnotation<Object>> annotations) {
        for (PreAnnotation pa : annotations) {
            if (pa != null) {
                return pa;
            }
        }
        return null;
    }

}
