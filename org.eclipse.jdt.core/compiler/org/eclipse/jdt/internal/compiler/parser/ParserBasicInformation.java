/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *  
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.parser;

/*An interface that contains static declarations for some basic information
 about the parser such as the number of rules in the grammar, the starting state, etc...*/
public interface ParserBasicInformation {
    public final static int

      ERROR_SYMBOL      = 127,
      MAX_NAME_LENGTH   = 41,
      NUM_STATES        = 1166,

      NT_OFFSET         = 127,
      SCOPE_UBOUND      = 316,
      SCOPE_SIZE        = 317,
      LA_STATE_OFFSET   = 17298,
      MAX_LA            = 1,
      NUM_RULES         = 836,
      NUM_TERMINALS     = 127,
      NUM_NON_TERMINALS = 372,
      NUM_SYMBOLS       = 499,
      START_STATE       = 1150,
      EOFT_SYMBOL       = 64,
      EOLT_SYMBOL       = 64,
      ACCEPT_ACTION     = 17297,
      ERROR_ACTION      = 17298;
}
