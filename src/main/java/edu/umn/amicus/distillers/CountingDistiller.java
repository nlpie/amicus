package edu.umn.amicus.distillers;

import edu.umn.amicus.PreAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Distiller that will simply count the non-null annotations passed to it.
 * Can be used in an idiom with VotingDistiller and a Translator to enforce a minimum annotation threshold
 *      (e.g., do not write an output annotation unless at least n systems have one here).
 *
 * Created by gpfinley on 2/17/17.
 */
public class CountingDistiller implements AnnotationDistiller<Integer> {

    /**
     * Save all annotations into a list. Null values and annotations will be added to the list to maintain length
     *      predictable from the number of inputs.
     *
     * @param annotations
     */
    @Override
    public PreAnnotation<Integer> distill(List<PreAnnotation> annotations) {
        if (annotations.size() == 0) return null;

        int n = 0;
        Integer begin = null;
        Integer end = null;
        for (PreAnnotation pa : annotations) {
            if (pa != null) {
                n++;
                if (begin == null) {
                    begin = pa.getBegin();
                    end = pa.getEnd();
                }
            }
        }
        return new PreAnnotation<>(n, begin, end);
    }

}
