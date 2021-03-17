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
package net.automatalib.modelcheckers.m3c.formula.visitor;

import net.automatalib.modelcheckers.m3c.formula.AndNode;
import net.automatalib.modelcheckers.m3c.formula.BoxNode;
import net.automatalib.modelcheckers.m3c.formula.DiamondNode;
import net.automatalib.modelcheckers.m3c.formula.FalseNode;
import net.automatalib.modelcheckers.m3c.formula.FormulaNode;
import net.automatalib.modelcheckers.m3c.formula.NotNode;
import net.automatalib.modelcheckers.m3c.formula.OrNode;
import net.automatalib.modelcheckers.m3c.formula.TrueNode;
import net.automatalib.modelcheckers.m3c.formula.modalmu.GfpNode;
import net.automatalib.modelcheckers.m3c.formula.modalmu.LfpNode;
import net.automatalib.modelcheckers.m3c.formula.modalmu.VariableNode;
import net.automatalib.modelcheckers.m3c.formula.parser.ParseException;
import net.automatalib.modelcheckers.m3c.formula.parser.ParserMuCalc;
import org.testng.Assert;
import org.testng.annotations.Test;

public class NNFVisitorTest {

    @Test
    void testBaseCases() throws ParseException {
        NNFVisitor nnfVisitor = new NNFVisitor();

        FormulaNode atomicNode = ParserMuCalc.parse("! \"a\"");
        FormulaNode nnfAtomicNode = nnfVisitor.transformToNNF(atomicNode);
        Assert.assertEquals(atomicNode, nnfAtomicNode);

        FormulaNode trueNode = ParserMuCalc.parse("! true");
        FormulaNode nnfTrueNode = nnfVisitor.transformToNNF(trueNode);
        Assert.assertEquals(new FalseNode(), nnfTrueNode);

        FormulaNode falseNode = ParserMuCalc.parse("! false");
        FormulaNode nnfFalseNode = nnfVisitor.transformToNNF(falseNode);
        Assert.assertEquals(new TrueNode(), nnfFalseNode);

        testGfp();
        testLfp();
        testAnd();
        testOr();
        testBoxNode();
        testDiamondNode();
        testDefaultExample();
    }

    private void testGfp() throws ParseException {
        FormulaNode gfpNode = ParserMuCalc.parse("! (nu X.(false || X))");
        FormulaNode nnfGfpNode = gfpNode.toNNF();

        /* Create (mu X.(true & X)*/
        LfpNode lfpNode = new LfpNode("X", new AndNode(new TrueNode(), new VariableNode("X")));
        Assert.assertEquals(lfpNode, nnfGfpNode);
    }

    private void testLfp() throws ParseException {
        FormulaNode lfpNode = ParserMuCalc.parse("! (mu X.(false || !X))");
        FormulaNode nnfLfpNode = lfpNode.toNNF();

        /* Create nu X.(true & !X) */
        GfpNode gfpNode = new GfpNode("X", new AndNode(new TrueNode(), new NotNode(new VariableNode("X"))));
        Assert.assertEquals(gfpNode, nnfLfpNode);
    }

    private void testAnd() throws ParseException {
        FormulaNode andNode = ParserMuCalc.parse("!(<> false && true)");
        FormulaNode nnfAndNode = andNode.toNNF();

        /* Create ([]true | false) */
        OrNode orNode = new OrNode(new BoxNode("", new TrueNode()), new FalseNode());
        Assert.assertEquals(orNode, nnfAndNode);
    }

    private void testOr() throws ParseException {
        FormulaNode orNode = ParserMuCalc.parse("!([a] false || true)");
        FormulaNode nnfOrNode = orNode.toNNF();

        /* Create (<a> true & false) */
        AndNode andNode = new AndNode(new DiamondNode("a", new TrueNode()), new FalseNode());
        Assert.assertEquals(andNode, nnfOrNode);
    }

    private void testBoxNode() throws ParseException {
        FormulaNode boxNode = ParserMuCalc.parse("![a]true");
        FormulaNode nnfBoxNode = boxNode.toNNF();

        /* Create (<a>false)*/
        DiamondNode diamondNode = new DiamondNode("a", new FalseNode());
        Assert.assertEquals(diamondNode, nnfBoxNode);
    }

    private void testDiamondNode() throws ParseException {
        FormulaNode diamondNode = ParserMuCalc.parse("!<a>false");
        FormulaNode nnfDiamondNode = diamondNode.toNNF();

        /* Create ([a] true) */
        BoxNode boxNode = new BoxNode("a", new TrueNode());
        Assert.assertEquals(boxNode, nnfDiamondNode);
    }

    private void testDefaultExample() throws ParseException {
        FormulaNode ast = ParserMuCalc.parse("!(mu X.(<b><b>true || <>X))");
        FormulaNode nnfAst = ast.toNNF();

        /* Create nu X.([b][b]false & []X)*/
        GfpNode gfpNode = new GfpNode("X",
                                      new AndNode(new BoxNode("b", new BoxNode("b", new FalseNode())),
                                                  new BoxNode("", new VariableNode("X"))));
        Assert.assertEquals(gfpNode, nnfAst);
    }

}
