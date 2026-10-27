package com.orudesk.orudesk;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * The same check as in the core, now across repositories: donors may use only the core modules' public
 * APIs (identity's top-level package, party's port.in, shared's top-level package), never their internals.
 */
class ModularityTest {

    @Test
    void modulesOnlyUseEachOthersPublicApi() {
        ApplicationModules.of(OrudeskNgoApplication.class).verify();
    }
}
