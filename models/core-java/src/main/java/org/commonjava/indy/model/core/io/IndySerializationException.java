/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.model.core.io;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;

public class IndySerializationException
    extends JsonProcessingException
{

    public IndySerializationException( final String msg, final JsonLocation loc, final Throwable rootCause )
    {
        super( msg, loc, rootCause );
    }

    public IndySerializationException( final String msg, final JsonLocation loc )
    {
        super( msg, loc );
    }

    private static final long serialVersionUID = 1L;

}
