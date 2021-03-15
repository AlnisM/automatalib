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
package net.automatalib.util.automata.conformance;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import net.automatalib.automata.UniversalDeterministicAutomaton;
import net.automatalib.commons.util.collections.AbstractThreeLevelIterator;
import net.automatalib.commons.util.collections.CollectionsUtil;
import net.automatalib.commons.util.collections.ReusableIterator;
import net.automatalib.commons.util.mappings.MutableMapping;
import net.automatalib.util.automata.Automata;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.util.automata.equivalence.CharacterizingSets;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Iterator that returns test words generated by the partial W method.
 * <p>
 * See "Test selection based on finite state models" by S. Fujiwara et al.
 *
 * @param <I>
 *         input symbol type
 *
 * @author frohme
 */
public class WpMethodTestsIterator<I> extends ForwardingIterator<Word<I>> {

    private final Iterator<Word<I>> wpIterator;

    /**
     * Convenience-constructor for {@link #WpMethodTestsIterator(UniversalDeterministicAutomaton, Collection, int)} that
     * selects {@code 0} as {@code maxDepth}.
     *
     * @param automaton
     *         the automaton for which the testing sequences should be generated
     * @param inputs
     *         the input symbols that should be considered for test sequence generation
     */
    public WpMethodTestsIterator(UniversalDeterministicAutomaton<?, I, ?, ?, ?> automaton,
                                 Collection<? extends I> inputs) {
        this(automaton, inputs, 0);
    }

    /**
     * Constructor.
     *
     * @param automaton
     *         the automaton for which the testing sequences should be generated
     * @param inputs
     *         the input symbols that should be considered for test sequence generation
     * @param maxDepth
     *         the maximum number of symbols that are appended to the transition-cover part of the test sequences
     */
    public WpMethodTestsIterator(UniversalDeterministicAutomaton<?, I, ?, ?, ?> automaton,
                                 Collection<? extends I> inputs,
                                 int maxDepth) {

        final Set<Word<I>> stateCover = Sets.newHashSetWithExpectedSize(automaton.size());
        final Set<Word<I>> transitionCover = Sets.newHashSetWithExpectedSize(automaton.size() * inputs.size());

        Covers.cover(automaton, inputs, stateCover, transitionCover);

        final Iterable<Word<I>> characterizingSet;
        final Iterator<Word<I>> characterizingIter = CharacterizingSets.characterizingSetIterator(automaton, inputs);

        // Special case: List of characterizing suffixes may be empty,
        // but in this case we still need to iterate over the prefixes!
        if (!characterizingIter.hasNext()) {
            characterizingSet = Collections.singletonList(Word.epsilon());
        } else {
            characterizingSet = new ReusableIterator<>(characterizingIter);
        }

        // Phase 1: state cover * middle part * global suffixes
        final Iterator<Word<I>> firstIterator =
                new FirstPhaseIterator<>(stateCover, CollectionsUtil.allTuples(inputs, 0, maxDepth), characterizingSet);

        // Phase 2: transitions (not in state cover) * middle part * local suffixes
        transitionCover.removeAll(stateCover);
        final Iterator<Word<I>> secondIterator = new SecondPhaseIterator<>(automaton,
                                                                           inputs,
                                                                           transitionCover,
                                                                           CollectionsUtil.allTuples(inputs,
                                                                                                     0,
                                                                                                     maxDepth));

        wpIterator = Iterators.concat(firstIterator, secondIterator);
    }

    @Override
    protected Iterator<Word<I>> delegate() {
        return wpIterator;
    }

    private static class FirstPhaseIterator<I> extends AbstractThreeLevelIterator<List<I>, Word<I>, Word<I>, Word<I>> {

        private final Iterable<Word<I>> prefixes;
        private final Iterable<Word<I>> suffixes;

        private final WordBuilder<I> wordBuilder = new WordBuilder<>();

        FirstPhaseIterator(Iterable<Word<I>> prefixes, Iterable<List<I>> middleParts, Iterable<Word<I>> suffixes) {
            super(middleParts.iterator());

            this.prefixes = prefixes;
            this.suffixes = suffixes;
        }

        @Override
        protected Iterator<Word<I>> l2Iterator(List<I> l1Object) {
            return prefixes.iterator();
        }

        @Override
        protected Iterator<Word<I>> l3Iterator(List<I> l1Object, Word<I> l2Object) {
            return suffixes.iterator();
        }

        @Override
        protected Word<I> combine(List<I> middle, Word<I> prefix, Word<I> suffix) {
            wordBuilder.ensureAdditionalCapacity(prefix.size() + middle.size() + suffix.size());
            Word<I> word = wordBuilder.append(prefix).append(middle).append(suffix).toWord();
            wordBuilder.clear();
            return word;
        }
    }

    private static class SecondPhaseIterator<S, I>
            extends AbstractThreeLevelIterator<List<I>, Word<I>, Word<I>, Word<I>> {

        private final UniversalDeterministicAutomaton<S, I, ?, ?, ?> automaton;
        private final Collection<? extends I> inputs;

        private final MutableMapping<S, @Nullable List<Word<I>>> localSuffixSets;
        private final Iterable<Word<I>> prefixes;

        private final WordBuilder<I> wordBuilder = new WordBuilder<>();

        SecondPhaseIterator(UniversalDeterministicAutomaton<S, I, ?, ?, ?> automaton,
                            Collection<? extends I> inputs,
                            Iterable<Word<I>> prefixes,
                            Iterable<List<I>> middleParts) {
            super(middleParts.iterator());

            this.automaton = automaton;
            this.inputs = inputs;
            this.localSuffixSets = automaton.createStaticStateMapping();
            this.prefixes = prefixes;
        }

        @Override
        protected Iterator<Word<I>> l2Iterator(List<I> l1Object) {
            return prefixes.iterator();
        }

        @Override
        protected Iterator<Word<I>> l3Iterator(List<I> middle, Word<I> prefix) {

            @SuppressWarnings("nullness") // input sequences have been computed on defined transitions
            final @NonNull S tmp = automaton.getState(prefix);
            @SuppressWarnings("nullness") // input sequences have been computed on defined transitions
            final @NonNull S state = automaton.getSuccessor(tmp, middle);

            @Nullable List<Word<I>> localSuffixes = localSuffixSets.get(state);

            if (localSuffixes == null) {
                localSuffixes = Automata.stateCharacterizingSet(automaton, inputs, state);
                if (localSuffixes.isEmpty()) {
                    localSuffixes = Collections.singletonList(Word.epsilon());
                }
                localSuffixSets.put(state, localSuffixes);
            }

            return localSuffixes.iterator();
        }

        @Override
        protected Word<I> combine(List<I> middle, Word<I> prefix, Word<I> suffix) {
            wordBuilder.ensureAdditionalCapacity(prefix.size() + middle.size() + suffix.size());
            Word<I> word = wordBuilder.append(prefix).append(middle).append(suffix).toWord();
            wordBuilder.clear();
            return word;
        }
    }
}
