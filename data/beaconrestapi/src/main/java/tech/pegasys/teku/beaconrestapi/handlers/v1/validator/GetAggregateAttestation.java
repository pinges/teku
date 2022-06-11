/*
 * Copyright ConsenSys Software Inc., 2022
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

package tech.pegasys.teku.beaconrestapi.handlers.v1.validator;

import static tech.pegasys.teku.beaconrestapi.BeaconRestApiTypes.ATTESTATION_DATA_ROOT_PARAMETER;
import static tech.pegasys.teku.beaconrestapi.BeaconRestApiTypes.SLOT_PARAMETER;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.ATTESTATION_DATA_ROOT;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.RES_BAD_REQUEST;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.RES_FORBIDDEN;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.RES_INTERNAL_ERROR;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.RES_NOT_FOUND;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.RES_OK;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.SLOT;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.TAG_VALIDATOR;
import static tech.pegasys.teku.infrastructure.http.RestApiConstants.TAG_VALIDATOR_REQUIRED;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import java.util.Optional;
import java.util.function.Function;
import org.apache.tuweni.bytes.Bytes32;
import org.jetbrains.annotations.NotNull;
import tech.pegasys.teku.api.DataProvider;
import tech.pegasys.teku.api.ValidatorDataProvider;
import tech.pegasys.teku.api.response.v1.validator.GetAggregatedAttestationResponse;
import tech.pegasys.teku.beaconrestapi.MigratingEndpointAdapter;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.http.HttpStatusCodes;
import tech.pegasys.teku.infrastructure.json.types.SerializableTypeDefinition;
import tech.pegasys.teku.infrastructure.restapi.endpoints.AsyncApiResponse;
import tech.pegasys.teku.infrastructure.restapi.endpoints.EndpointMetadata;
import tech.pegasys.teku.infrastructure.restapi.endpoints.ParameterMetadata;
import tech.pegasys.teku.infrastructure.restapi.endpoints.RestApiRequest;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.config.SpecConfig;
import tech.pegasys.teku.spec.datastructures.operations.Attestation;

public class GetAggregateAttestation extends MigratingEndpointAdapter {
  public static final String ROUTE = "/eth/v1/validator/aggregate_attestation";
  private final ValidatorDataProvider provider;

  private static final ParameterMetadata<UInt64> SLOT_PARAM =
      SLOT_PARAMETER.withDescription(
          "`uint64` Non-finalized slot for which to create the aggregation.");

  public GetAggregateAttestation(final DataProvider dataProvider, final Spec spec) {
    this(dataProvider.getValidatorDataProvider(), spec);
  }

  public GetAggregateAttestation(final ValidatorDataProvider provider, final Spec spec) {
    super(
        EndpointMetadata.get(ROUTE)
            .operationId("getAggregateAttestation")
            .summary("Get aggregated attestations")
            .description(
                "Aggregates all attestations matching given attestation data root and slot.")
            .tags(TAG_VALIDATOR, TAG_VALIDATOR_REQUIRED)
            .queryParamRequired(ATTESTATION_DATA_ROOT_PARAMETER)
            .queryParamRequired(SLOT_PARAM)
            .response(
                HttpStatusCodes.SC_OK,
                "Request successful",
                getResponseType(spec.getGenesisSpecConfig()))
            .withNotFoundResponse()
            .build());
    this.provider = provider;
  }

  @OpenApi(
      path = ROUTE,
      method = HttpMethod.GET,
      summary = "Get aggregated attestations",
      description = "Aggregates all attestations matching given attestation data root and slot.",
      tags = {TAG_VALIDATOR, TAG_VALIDATOR_REQUIRED},
      queryParams = {
        @OpenApiParam(
            name = ATTESTATION_DATA_ROOT,
            description =
                "`String` HashTreeRoot of AttestationData that validator wants aggregated.",
            required = true),
        @OpenApiParam(
            name = SLOT,
            description = "`uint64` Non-finalized slot for which to create the aggregation.",
            required = true)
      },
      responses = {
        @OpenApiResponse(
            status = RES_OK,
            content = @OpenApiContent(from = GetAggregatedAttestationResponse.class),
            description =
                "Returns aggregated `Attestation` object with same `AttestationData` root."),
        @OpenApiResponse(status = RES_BAD_REQUEST, description = "Invalid parameter supplied"),
        @OpenApiResponse(
            status = RES_NOT_FOUND,
            description = "No matching attestations were found"),
        @OpenApiResponse(
            status = RES_FORBIDDEN,
            description = "Beacon node was not assigned to aggregate on that subnet"),
        @OpenApiResponse(status = RES_INTERNAL_ERROR, description = "Beacon node internal error.")
      })
  @Override
  public void handle(@NotNull Context ctx) throws Exception {
    adapt(ctx);
  }

  @Override
  public void handleRequest(RestApiRequest request) throws JsonProcessingException {
    final Bytes32 beaconBlockRoot = request.getQueryParameter(ATTESTATION_DATA_ROOT_PARAMETER);
    final UInt64 slot = request.getQueryParameter(SLOT_PARAM);

    final SafeFuture<Optional<Attestation>> future =
        provider.createAggregate(slot, beaconBlockRoot);

    request.respondAsync(
        future.thenApply(
            maybeAttestation ->
                maybeAttestation
                    .map(AsyncApiResponse::respondOk)
                    .orElseGet(AsyncApiResponse::respondNotFound)));
  }

  private static SerializableTypeDefinition<Attestation> getResponseType(SpecConfig specConfig) {
    Attestation.AttestationSchema dataSchema = new Attestation.AttestationSchema(specConfig);

    return SerializableTypeDefinition.object(Attestation.class)
        .name("GetAggregatedAttestationResponse")
        .withField("data", dataSchema.getJsonTypeDefinition(), Function.identity())
        .build();
  }
}
