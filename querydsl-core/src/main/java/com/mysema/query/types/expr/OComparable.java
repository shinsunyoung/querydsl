/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.types.expr;

import java.util.Arrays;
import java.util.List;

import com.mysema.query.types.Expr;
import com.mysema.query.types.Operation;
import com.mysema.query.types.Operator;
import com.mysema.query.types.Visitor;

/**
 * OComparable represents Comparable operations
 *
 * @author tiwe
 *
 * @param <D>
 */
public class OComparable<D extends Comparable<?>> extends
        EComparable<D> implements Operation<D> {

    private static final long serialVersionUID = 1129243977606098865L;

    /**
     * Factory method
     *
     * @param <D>
     * @param type
     * @param op
     * @param args
     * @return
     */
    public static <D extends Comparable<?>> EComparable<D> create(Class<D> type, Operator<? super D> op, Expr<?>... args){
        return new OComparable<D>(type, op, args);
    }

    private final Operation<D> opMixin;

    OComparable(Class<D> type, Operator<? super D> op, Expr<?>... args) {
        this(type, op, Arrays.asList(args));
    }

    OComparable(Class<D> type, Operator<? super D> op, List<Expr<?>> args) {
        super(type);
        this.opMixin = new OperationMixin<D>(this, op, args);
    }

    @Override
    public <R,C> R accept(Visitor<R,C> v, C context) {
        return v.visit(this, context);
    }

    @Override
    public Expr<?> getArg(int index) {
        return opMixin.getArg(index);
    }

    @Override
    public List<Expr<?>> getArgs() {
        return opMixin.getArgs();
    }

    @Override
    public Operator<? super D> getOperator() {
        return opMixin.getOperator();
    }

    @Override
    public boolean equals(Object o){
        return opMixin.equals(o);
    }

    @Override
    public int hashCode(){
        return getType().hashCode();
    }

}
