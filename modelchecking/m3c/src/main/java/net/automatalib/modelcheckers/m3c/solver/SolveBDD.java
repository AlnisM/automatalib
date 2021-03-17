/* Copyright (C) 2013-2021 TU Dortmund
 * This file is part of AutomataLib, http://www.automatalib.net/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.automatalib.modelcheckers.m3c.solver;

import info.scce.addlib.dd.bdd.BDDManager;
import net.automatalib.graphs.ModalContextFreeProcessSystem;
import net.automatalib.modelcheckers.m3c.formula.FormulaNode;
import net.automatalib.modelcheckers.m3c.formula.parser.ParseException;
import net.automatalib.modelcheckers.m3c.formula.parser.ParserCTL;
import net.automatalib.modelcheckers.m3c.formula.parser.ParserMuCalc;
import net.automatalib.modelcheckers.m3c.transformer.BDDTransformer;
import net.automatalib.ts.modal.transition.ModalEdgeProperty;

public class SolveBDD<L, AP> extends SolveDD<BDDTransformer<L, AP>, L, AP> {

    private BDDManager bddManager;

    SolveBDD(ModalContextFreeProcessSystem<L, AP> mcfps, FormulaNode<L, AP> formula, boolean formulaIsCtl) {
        super(mcfps, formula, formulaIsCtl);
    }

    @Override
    protected void initDDManager() {
        this.bddManager = new BDDManager();
    }

    @Override
    protected BDDTransformer<L, AP> createInitTransformerEnd() {
        return new BDDTransformer<>(bddManager, dependGraph.getNumVariables());
    }

    @Override
    protected BDDTransformer<L, AP> createInitState() {
        return new BDDTransformer<>(bddManager, dependGraph);
    }

    @Override
    protected <TP extends ModalEdgeProperty> BDDTransformer<L, AP> createInitTransformerEdge(L edgeLabel, TP edgeProperty) {
        return new BDDTransformer<>(bddManager, edgeLabel, edgeProperty, dependGraph);
    }

    public static <L, AP> SolveBDD<L, AP> solver(ModalContextFreeProcessSystem<L, AP> mcfps,
                                                 FormulaNode<L, AP> formula,
                                                 boolean formulaIsCtl) throws ParseException {
        return new SolveBDD<>(mcfps, formula, formulaIsCtl);
    }

    public static SolveBDD<String, String> solver(ModalContextFreeProcessSystem<String, String> mcfps,
                                                  String formula,
                                                  boolean formulaIsCtl) throws ParseException {
        return new SolveBDD<>(mcfps,
                              formulaIsCtl ? ParserCTL.parse(formula) : ParserMuCalc.parse(formula),
                              formulaIsCtl);
    }
}
