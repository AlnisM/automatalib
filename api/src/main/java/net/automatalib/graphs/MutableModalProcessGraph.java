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
package net.automatalib.graphs;

import java.util.Collections;
import java.util.Set;

import net.automatalib.graphs.concepts.MutableEdgeLabels;
import net.automatalib.graphs.concepts.MutableKripkeInterpretation;
import net.automatalib.ts.modal.transition.MutableModalEdgeProperty;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface MutableModalProcessGraph<N, L, E, AP, TP extends MutableModalEdgeProperty>
        extends ModalProcessGraph<N, L, E, AP, TP>,
                MutableGraph<N, E, Set<AP>, TP>,
                MutableKripkeInterpretation<N, AP>,
                MutableEdgeLabels<E, L> {

    void setInitialNode(@Nullable N initialNode);

    void setFinalNode(@Nullable N finalNode);

    @Override
    default void setAtomicPropositions(N node, Set<AP> atomicPropositions) {
        setNodeProperty(node, atomicPropositions);
    }

    @Override
    default N addNode() {
        return addNode(Collections.emptySet());
    }
}
