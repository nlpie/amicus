package edu.umn.amicus.distillers;

import edu.umn.amicus.AlignedTuple;
import edu.umn.amicus.PreAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gpfinley on 2/17/17.
 */
public class ListDistiller implements Distiller<List> {

    /**
     * Save all annotations into a list. Null values and annotations will be added to the list to maintain length
     *      predictable from the number of inputs.
     *
     * @param annotations
     */
    @Override
    public PreAnnotation<List> distill(AlignedTuple<PreAnnotation<List>> annotations) {
        if (annotations.size() == 0) return null;

        List<Object> distilledList = new ArrayList<>();

        int begin = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;

        for (PreAnnotation preAnnot : annotations) {
            if (preAnnot != null) {
                distilledList.add(preAnnot.getValue());
                if (preAnnot.getEnd() > end) end = preAnnot.getEnd();
                if (preAnnot.getBegin() < begin) begin = preAnnot.getBegin();
            } else {
                distilledList.add(null);
            }
        }
        return new PreAnnotation<List>(distilledList, begin, end);
    }

}
