/**
 * Copyright (c) 2009-2013, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.repo;

import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.rultor.spi.Repo;
import com.rultor.spi.User;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

/**
 * Reference.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.0
 */
@Immutable
@ToString
@EqualsAndHashCode(of = { "grammar", "name" })
@Loggable(Loggable.DEBUG)
final class Reference implements Variable<Object> {

    /**
     * Grammar where to look for vars.
     */
    private final transient Grammar grammar;

    /**
     * The name.
     */
    private final transient String name;

    /**
     * Public ctor.
     * @param grm Grammar to use
     * @param ref Reference
     */
    protected Reference(final Grammar grm, final String ref) {
        Validate.matchesPattern(ref, "[-_\\w]+");
        this.grammar = grm;
        this.name = ref;
    }

    /**
     * {@inheritDoc}
     * @checkstyle RedundantThrows (5 lines)
     */
    @Override
    public Object instantiate(final User user)
        throws Repo.InstantiationException {
        if (!user.units().contains(this.name)) {
            throw new Repo.InstantiationException(
                String.format(
                    "unit '%s' not found in your account",
                    this.name
                )
            );
        }
        try {
            return this.alter(
                this.grammar.parse(
                    user.get(this.name).spec().asText()
                ).instantiate(user)
            );
        } catch (Repo.InvalidSyntaxException ex) {
            throw new Repo.InstantiationException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String asText() {
        return this.name;
    }

    /**
     * Alter the object by injecting name into it.
     * @param object The object
     * @return Altered object
     * @throws Repo.InstantiationException If some error inside
     * @checkstyle RedundantThrows (5 lines)
     */
    private Object alter(final Object object)
        throws Repo.InstantiationException {
        for (Method method : object.getClass().getMethods()) {
            if (method.getName().equals(Composite.METHOD)) {
                try {
                    method.invoke(object, this.name);
                } catch (IllegalAccessException ex) {
                    throw new Repo.InstantiationException(ex);
                } catch (SecurityException ex) {
                    throw new Repo.InstantiationException(ex);
                } catch (InvocationTargetException ex) {
                    throw new Repo.InstantiationException(ex);
                }
            }
        }
        return object;
    }

}
