/*
This file is part of the Sulfur project by Ivan De Marino (http://ivandemarino.me).

Copyright (c) 2013, Ivan De Marino (http://ivandemarino.me)
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;

import static org.testng.Assert.*;

public class JSONConfBuilderTest {

    @Test
    public void shouldLoadDefaultConfiguration() {
        JSONConf c = new JSONConfBuilder("default-config.json").build();

        // Check it does return an instance of JSONConf
        assertNotNull(c);

        // Check some values on it
        assertEquals(c.getValue("name"), "default-config");
        assertEquals(c.getValue("shared.shared_field_num"), 1);
        assertEquals(c.getValue("shared.shared_field_obj.value"), "default-config");

        // Check it can generate a Child JSONConf
        JSONConf child = c.getChild("shared");
        assertNotNull(child);

        // Check the child JSONConf values
        assertEquals(child.getValue("shared_field_num"), 1);
        assertEquals(child.getValue("shared_field_obj.value"), null);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void shouldThrowExceptionIfDefaultConfigurationNotFound() {
        JSONConf c = new JSONConfBuilder("default-config-that-does-not-exist.json").build();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void shouldThrowExceptionIfChildJsonConfDoesNotExist() {
        JSONConf c = new JSONConfBuilder("default-config.json").build();
        c.getChild("non-existent");
    }

    @Test
    public void shouldLoadDefaultAndUserConfiguration() {
        JSONConf c = new JSONConfBuilder("default-config.json", "test-fixtures/config.json").build();

        // Check it does return an instance of JSONConf
        assertNotNull(c);

        // Check some values on it
        assertEquals(c.getValue("name"), "user-config");
        assertEquals(c.getValue("shared.shared_field_num"), 2);
        assertEquals(c.getValue("shared.shared_field_obj.value"), "user-config");

        // Check it can generate a Child JSONConf
        JSONConf child = c.getChild("shared");
        assertNotNull(child);

        // Check the child JSONConf values
        assertEquals(child.getValue("shared_field_num"), 2);
        assertEquals(child.getValue("shared_field_obj.value"), null);
    }

    @DataProvider(name = "provideCLIPropertiesArrayNames")
    public Object[][] CLIPropertiesArrayNames() {
        return new Object[][] {
                { JSONConfBuilder.DEFAULT_CLI_PROPERTIES_ARRAY_NAME },
                { "anotherCliArrName" }
        };
    }

    @Test(dataProvider = "provideCLIPropertiesArrayNames")
    public void shouldLoadDefaultUserAndCliConfiguration(String cliPropsArrayName) {
        Properties sysProps = new Properties();

        sysProps.setProperty(cliPropsArrayName + "[0]", "name=\"cli config\"");
        sysProps.setProperty(cliPropsArrayName + "[1]", "shared.shared_field_num=3");
        sysProps.setProperty(cliPropsArrayName + "[2]", "['annoyingly long string'].browsers=\"firefox\"");
        sysProps.setProperty(cliPropsArrayName + "[3]", "array_of_nums=[1, 2, 3]");
        sysProps.setProperty(cliPropsArrayName + "[4]", "array_of_strings=[\"string1\", \"string2\", \"string3\"]");

        JSONConf c = new JSONConfBuilder("default-config.json")
                .withUserConfFilePath("test-fixtures/config.json")
                .withSystemProperties(sysProps)
                .withCLIPropsArray(cliPropsArrayName)
                .build();

        // Check it does return an instance of JSONConf
        assertNotNull(c);

        // Check some values on it
        assertEquals(c.getValue("name"), "cli config");
        assertEquals(c.getValue("shared.shared_field_num"), 3);
        assertEquals(c.getValue("shared.shared_field_obj.value"), "cli config");
        assertEquals(c.getValue("['annoyingly long string'].browsers"), "firefox");
        List<Integer> array_of_nums = c.getValue("array_of_nums");
        assertTrue(array_of_nums.get(0) == 1);
        assertTrue(array_of_nums.get(1) == 2);
        assertTrue(array_of_nums.get(2) == 3);
        List<String> array_of_strings = c.getValue("array_of_strings");
        assertEquals(array_of_strings.get(0), "string1");
        assertEquals(array_of_strings.get(1), "string2");
        assertEquals(array_of_strings.get(2), "string3");
    }
}
