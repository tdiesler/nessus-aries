/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package io.nessus.aries.test;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.GetNymRoleResponse;
import org.hyperledger.acy_py.generated.model.GetNymRoleResponse.RoleEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test the Acapy wallet endpoint 
 */
public class WalletTest extends AbstractAriesTest {

	@Test
	public void testTrusteeDid() throws Exception {

		DID didRes = ac.walletDidPublic().get();
		log.info("" + didRes);
		Assertions.assertEquals(TRUSTEE_DID, didRes.getDid());
		Assertions.assertEquals(TRUSTEE_VKEY, didRes.getVerkey());
		
		GetNymRoleResponse nymRoleRes = ac.ledgerGetNymRole(TRUSTEE_DID).get();
		log.info("" + nymRoleRes);
		Assertions.assertEquals(RoleEnum.TRUSTEE, nymRoleRes.getRole());
	}
}
