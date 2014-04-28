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

import com.google.gson.*;
import com.jayway.jsonpath.internal.PathToken;
import com.jayway.jsonpath.internal.PathTokenizer;

import java.io.*;
import java.util.*;

/**
 * This Builder helps to create a JSONConf object.
 * It can be tuned to the specific needs of your code, and can assemble together
 * (via algebraic UNION operation), all the JSON files given to it.
 * <p/>
 *
 * There is only one mandatory parameter: 1 "default" JSON file.
 * After that, everything is optional.
 * <p/>
 *
 * Particular attention needs to be given to the "user" JSON files.
 * The final JSONConf can be the UNION of many of those: the order in which
 * this UNION is executed is based on the order in which they are provided.
 */
public class JSONConfBuilder {

    public static final String DEFAULT_CLI_PROPERTIES_ARRAY_NAME = "json";

    private static final Gson DEFAULT_GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private String defaultConfFilePath;
    private List<String> userConfFilePaths = new ArrayList<String>();
    private Properties sysProps = System.getProperties();
    private String CLIPropsArrayName = DEFAULT_CLI_PROPERTIES_ARRAY_NAME;
    private Gson gson = DEFAULT_GSON;

    /**
     * Creates a ConfigurationBuilder (Builder Pattern)
     *
     * @param defaultConfFilePath Path to the Default Configuration File.
     *                            "null" string will determine an empty (but valid) configuration file.
     */
    public JSONConfBuilder(String defaultConfFilePath) {
        this.defaultConfFilePath = defaultConfFilePath;
    }

    /**
     * Creates a ConfigurationBuilder (Builder Pattern)
     *
     * @param defaultConfFilePath Path to the Default Configuration File.
     *                            "null" string will determine an empty (but valid) configuration file.
     * @param userConfFilePaths   Path to the User Configuration Files
     *                            "null" string will determine an empty (but valid) configuration file.
     */
    public JSONConfBuilder(String defaultConfFilePath, String... userConfFilePaths) {
        this.defaultConfFilePath = defaultConfFilePath;
        this.withUserConfFilePath(userConfFilePaths);
    }

    /**
     * Provide paths to a User Configuration Files.
     *
     * The order in which files are added DOES influence the order in which
     * files are united.
     * This can be used multiple times: every time the Paths are added in
     * queue to the list of User Conf files provided so far.
     *
     * @param userConfFilePaths Path to the User Configuration File
     *                         "null" string will determine an empty (but valid) configuration file.
     * @return Same ConfigurationBuilder instance (for chaining)
     */
    public JSONConfBuilder withUserConfFilePath(String... userConfFilePaths) {
        this.userConfFilePaths.addAll(Arrays.asList(userConfFilePaths));
        return this;
    }

    /**
     * Provide Properties in which to look for CLI Properties Array.
     * If not configured, this builder will use {@link System#getProperties()}
     *
     * @param sysProps Properties container
     * @return Same ConfigurationBuilder instance (for chaining)
     */
    public JSONConfBuilder withSystemProperties(Properties sysProps) {
        this.sysProps = sysProps;
        return this;
    }

    /**
     * Provide name of the CLI Properties Array to look for within given System Properties.
     *
     * @param propsArrayName Name of the Properties Array to look for within the given System Properties.
     * @return Same ConfigurationBuilder instance (for chaining)
     */
    public JSONConfBuilder withCLIPropsArray(String propsArrayName) {
        this.CLIPropsArrayName = propsArrayName;
        return this;
    }

    /**
     * Provide a specific Gson instance to use while creating/converting JSON.
     *
     * @param gson An instance of Gson
     * @return Same ConfigurationBuilder instance (for chaining)
     */
    public JSONConfBuilder withGson(Gson gson) {
        this.gson = gson;
        return this;
    }

    /**
     * Builds the Configuration, based on the given parameters.
     *
     * The provided JSON files will be UNITED one at a time, in the order
     * they have been provided.
     *
     * @return New Configuration, based on the given parameters.
     */
    public JSONConf build() {
        // Start from the default configuration
        JsonObject result = loadJsonFromFile(defaultConfFilePath);

        // United the User configuration (one at a time, if any)
        for (String userConfFilePath : userConfFilePaths) {
            result = union(result, loadJsonFromFile(userConfFilePath));
        }

        // Unite CLI Configuration (one at a time, if any)
        JsonObject cliConf = new JsonObject();
        int idx = 0;
        String idxFormat = CLIPropsArrayName + "[%d]";
        while (sysProps.getProperty(String.format(idxFormat, idx)) != null) {
            result = union(
                    result,
                    jsonPathAssignmentToJsonObject(sysProps.getProperty(String.format(idxFormat, idx++))));
        }

        return new JSONConf(result);
    }

    /**
     * Loads a JsonObject from a FilePath.
     * NOTE: The filePath will be first searched within the Project Resources,
     * then on the Filesystem as a RELATIVE path.
     *
     * @param filePath (Relative) Path to JSON File we want to load
     * @return JsonObject from the given file
     */
    protected JsonObject loadJsonFromFile(String filePath) {
        if (null == filePath) {
            return new JsonObject();
        }

        // Work out the actual file location
        // Look within the project resources
        InputStream is = JSONConfBuilder.class.getClassLoader().getResourceAsStream(filePath);

        Reader fileReader = null;
        try {
            if (null != is) {
                // File is within the resources of the project
                fileReader = new InputStreamReader(is);
            } else {
                // File not within the resources of the project
                if (!new File(filePath).exists()) {
                    throw new FileNotFoundException(filePath);
                }
                fileReader = new FileReader(filePath);
            }
            return gson.fromJson(fileReader, JsonObject.class);
        } catch (FileNotFoundException fnfe) {
            throw new RuntimeException(fnfe);
        } finally {
            try {
                if (null != fileReader) fileReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }


    /**
     * Algebraic Union of 2 JsonObjects.
     *
     * @param A JsonObject "A"
     * @param B JsonObject "B"
     * @return A JsonObject containing all the fields of A-union-B
     */
    protected static JsonObject union(JsonObject A, JsonObject B) {
        JsonObject result = new JsonObject();

        // First, copy everything from A
        for (Map.Entry<String, JsonElement> entryA : A.entrySet()) {
            result.add(entryA.getKey(), entryA.getValue());
        }

        // Then, add content from B - recursively if needed
        for (Map.Entry<String, JsonElement> entryB : B.entrySet()) {
            String keyB = entryB.getKey();
            JsonElement valueB = entryB.getValue();

            if (A.has(keyB) && (A.get(keyB).isJsonObject() && valueB.isJsonObject())) {
                // This entry in B is also in A
                result.add(keyB, union(A.get(keyB).getAsJsonObject(), valueB.getAsJsonObject()));
            } else {
                // This entry in B is not in A: we just need to copy it over
                result.add(keyB, valueB);
            }
        }

        return result;
    }

    /**
     * Algebraic Union of "n" JsonObjects.
     *
     * @param objects Variable list of JsonObjects
     * @return A JsonObject containing the Union of all Objects, applied in order.
     */
    protected static JsonObject union(JsonObject... objects) {
        if (objects.length == 0) {
            // Returns an empty JsonObject if no input is provided
            return new JsonObject();
        }

        // Union all objects on the first one, then return it
        for (int i = 1, ilen = objects.length; i < ilen; ++i) {
            objects[0] = union(objects[0], objects[i]);
        }
        return objects[0];
    }

    protected static JsonObject intersection(JsonObject A, JsonObject B) {
        // TODO
        return null;
    }

    protected static JsonObject subtraction(JsonObject A, JsonObject B) {
        // TODO
        return null;
    }

    /**
     * Converts an assignment expressed via JsonPath into a JsonObject.
     * Expected format would be something like:
     * <pre>
     *     json.path.assignment=1
     * </pre>
     * <p/>
     * The resulting object would look like:
     * <pre>
     *     {
     *         "json" : {
     *             "path" : {
     *                 "assignment" : 1
     *             }
     *         }
     *     }
     * </pre>
     *
     * @param jsonPathAssignment JSON Path assignment
     * @return JSON Object result of the assignment
     */
    protected JsonObject jsonPathAssignmentToJsonObject(String jsonPathAssignment) {
        JsonObject result = new JsonObject();
        JsonObject current = result;
        JsonObject previous = null;
        String previousKey = null;

        // Use JsonPath to tokenize the given jsonPath and reconstruct a JsonObject
        PathTokenizer jsonPathTokenizer = new PathTokenizer(jsonPathAssignment);
        Iterator<PathToken> i = jsonPathTokenizer.iterator();
        i.next();   //< ignore "$", tha represent the root of a Json Path

        while (i.hasNext()) {
            PathToken token = i.next();
            if (token.isEndToken()) {
                // Reached the end of the Json Path.
                if (token.getFragment().contains("=")) {
                    String[] keyValue = token.getFragment().split("=");

                    // Here we MUST find an assignment, or throw an exception
                    if (keyValue.length != 2) {
                        throw new RuntimeException(String.format(
                                "JSON Path '%s' contains no assignment in last token '%s'",
                                jsonPathAssignment,
                                token.getFragment()));
                    }

                    // Add final "key=value"
                    String key = keyValue[0].replace("\"", "");
                    current.add(key, stringToJsonElement(keyValue[1]));
                } else {
                    // WORKAROUND: Need to use the previous object and key as the assignment symbol was
                    // wrongly assigned to the previous Token by the parser
                    previous.add(previousKey, stringToJsonElement(token.getFragment()));
                }
            } else {
                // Add another "key=object"
                JsonObject next = new JsonObject();

                // Remove quotes from string before storing
                String currentKey = token.getFragment().replace("\"", "");
                // Remove assignment from key, if found by tokenization
                if (currentKey.endsWith("=")) currentKey = currentKey.substring(0, currentKey.length() - 1);

                current.add(currentKey, next);

                // Move to the next object in the tree
                previous = current;
                previousKey = currentKey;
                current = next;
            }
        }

        return result;
    }

    /**
     * Converts a String to a valid JsonElement
     *
     * @param input A value that can be converted to a valid JSON element
     * @return A valid JsonElement, based on the input string
     */
    protected JsonElement stringToJsonElement(String input) {
        try {
            return gson.fromJson(input, JsonPrimitive.class);
        } catch (ClassCastException ccePrimitive) {
            try {
                return gson.fromJson(input, JsonArray.class);
            } catch (ClassCastException cceArray) {
                return gson.fromJson(input, JsonNull.class);
            }
        }
    }
}
