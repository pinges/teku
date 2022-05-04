/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.beaconrestapi.handlers.v1.beacon;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.pegasys.teku.infrastructure.http.HttpStatusCodes.SC_BAD_REQUEST;
import static tech.pegasys.teku.infrastructure.http.HttpStatusCodes.SC_INTERNAL_SERVER_ERROR;
import static tech.pegasys.teku.infrastructure.http.HttpStatusCodes.SC_OK;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.beaconrestapi.AbstractMigratedBeaconHandlerTest;
import tech.pegasys.teku.infrastructure.bytes.Bytes4;
import tech.pegasys.teku.infrastructure.restapi.endpoints.RestApiRequest;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.genesis.GenesisData;

public class GetGenesisTest extends AbstractMigratedBeaconHandlerTest {
  @Test
  public void shouldReturnUnavailableWhenStoreNotAvailable() throws Exception {
    final RestApiRequest request = mock(RestApiRequest.class);
    final GetGenesis handler = new GetGenesis(chainDataProvider);
    when(chainDataProvider.isStoreAvailable()).thenReturn(false);

    handler.handleRequest(request);
    verify(request).respondWithCode(SC_NOT_FOUND);
  }

  @Test
  public void shouldReturnGenesisInformation() throws Exception {
    final RestApiRequest request = mock(RestApiRequest.class);
    final GetGenesis handler = new GetGenesis(chainDataProvider);

    final UInt64 genesisTime = dataStructureUtil.randomUInt64();
    final Bytes32 genesisValidatorsRoot = dataStructureUtil.randomBytes32();

    final GenesisData expectedGenesisData = new GenesisData(genesisTime, genesisValidatorsRoot);
    when(chainDataProvider.isStoreAvailable()).thenReturn(true);
    when(chainDataProvider.getGenesisStateData()).thenReturn(expectedGenesisData);

    final GetGenesis.ResponseData expectedData =
        new GetGenesis.ResponseData(
            genesisTime, genesisValidatorsRoot, chainDataProvider.getGenesisForkVersion());

    handler.handleRequest(request);
    verify(request).respondOk(refEq(expectedData));
  }

  @Test
  void metadata_shouldHandle400() throws JsonProcessingException {
    verifyMetadataErrorResponse(new GetGenesis(chainDataProvider), SC_BAD_REQUEST);
  }

  @Test
  void metadata_shouldHandle404() {
    verifyMetadataEmptyResponse(new GetGenesis(chainDataProvider), SC_NOT_FOUND);
  }

  @Test
  void metadata_shouldHandle500() throws JsonProcessingException {
    verifyMetadataErrorResponse(new GetGenesis(chainDataProvider), SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  void metadata_shouldHandle200() throws JsonProcessingException {
    final UInt64 genesisTime = dataStructureUtil.randomUInt64();
    final Bytes32 genesisValidatorsRoot = dataStructureUtil.randomBytes32();
    final Bytes4 fork = dataStructureUtil.randomBytes4();
    final GetGenesis.ResponseData responseData =
        new GetGenesis.ResponseData(new GenesisData(genesisTime, genesisValidatorsRoot), fork);
    final String data =
        getResponseStringFromMetadata(new GetGenesis(chainDataProvider), SC_OK, responseData);
    assertThat(data)
        .isEqualTo(
            String.format(
                "{\"data\":{\"genesis_time\":\"%s\",\"genesis_validators_root\":\"%s\",\"genesis_fork_version\":\"%s\"}}",
                genesisTime, genesisValidatorsRoot, fork));
  }
}
