/*
This file is part of the JSONConf project by Ivan De Marino (http://ivandemarino.me).

Copyright (c) 2014, Ivan De Marino (http://ivandemarino.me)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.github.detro.jsonconf;

import com.google.gson.JsonPrimitive;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class JSONConfTest {

    @Test
    public void shouldSupportDataChangesToInternalJsonObject() {
        JSONConf c = new JSONConfBuilder("default-config.json").build();

        assertEquals(c.getValue("name"), "default-config");
        assertEquals(c.getValue("shared.shared_field_obj.key"), "key");
        assertEquals(c.getValue("shared.shared_field_obj.value"), "default-config");

        c.getInternalJsonObject().getAsJsonObject("shared").getAsJsonObject("shared_field_obj").add("key", new JsonPrimitive(10));
        c.getInternalJsonObject().getAsJsonObject("shared").getAsJsonObject("shared_field_obj").remove("value");

        assertEquals(c.getValue("shared.shared_field_obj.key"), 10);
        assertNull(c.getValue("shared.shared_field_obj.value"));

        c.getInternalJsonObject().getAsJsonObject("shared").getAsJsonObject("shared_field_obj").add("value", new JsonPrimitive("@shared.shared_field_num"));
        assertEquals(c.getValue("shared.shared_field_obj.value"), 1);
    }
}
