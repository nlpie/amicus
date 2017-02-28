package edu.umn.amicus.filters;

/**
 * No filtering.
 *
 * Created by gpfinley on 2/17/17.
 */
public class PassthroughFilter implements AnnotationFilter {

    @Override
    public boolean passes(Object value) {
        return true;
    }
}
