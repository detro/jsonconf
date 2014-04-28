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

import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;

public class JSONConf {

    private static final String REFERENCE_PREFIX = "@";

    private JsonObject jsonConfiguration;

    /**
     * JSONConf main class.
     * It "wraps" a JSON Object extra function focused on Configuration aspects.
     *
     * Instance of this represent a configuration, built using
     * {@link com.github.detro.jsonconf.JSONConfBuilder}.
     *
     * @param jsonCfg JSON Object to read configuration from.
     */
    public JSONConf(JsonObject jsonCfg) {
        if (null == jsonCfg || !jsonCfg.isJsonObject()) {
            throw new ClassCastException("Input JsonObject invalid or null");
        }
        this.jsonConfiguration = jsonCfg;
    }

    /**
     * Recovers a configuration value.
     * In the vast majority of time, this is just a configuration parameter.
     * But in facts this can be a JSON Path, in case the parameter the client code
     * is after is deep within the JSON structure.
     *
     * @param jsonPath JSON Path to a parameter
     * @param <T> Expected return type (JSON native types)
     * @return Parameter value, if found; "null" otherwise.
     */
    public <T> T getValue(String jsonPath) {
        T result = JsonPath.read(jsonConfiguration.toString(), jsonPath);

        // If result is a String and begins with the Reference Prefix, use it as input for recursive call
        if (result instanceof String && ((String) result).startsWith(REFERENCE_PREFIX)) {
            // TODO Detect loop???
            return getValue(((String) result).substring(1));
        }

        return result;
    }

    /**
     * Generates a child JSONConf object from child key, if found.
     * For example, if the original JSON for this Object is:
     * <pre>
     *     {
     *         "key" : "value",
     *         "childObject" : {
     *             "another_key" : "another_value",
     *             "chiave" : "valore"
     *         }
     *     }
     * </pre>
     *
     * A call to this method with "childObject" would generate a new JSONConf
     * based on the JSON:
     * <pre>
     *     {
     *          "another_key" : "another_value",
     *          "chiave" : "valore"
     *     }
     * </pre>
     *
     * @param childObjectKey Key of a child Object of the wrapped JSON Object
     * @return A new child JSONConf if the key exists; "null" otherwise.
     */
    public JSONConf getChild(String childObjectKey) {
        try {
            JsonObject childObj = jsonConfiguration.getAsJsonObject(childObjectKey);
            return new JSONConf(childObj);
        } catch (ClassCastException cce) {
            throw new RuntimeException(String.format("No Child Configuration '%s' found", childObjectKey), cce);
        }
    }

    @Override
    public String toString() {
        return jsonConfiguration.toString();
    }
}
