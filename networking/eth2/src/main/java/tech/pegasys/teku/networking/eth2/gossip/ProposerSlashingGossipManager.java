/*
 * Copyright Consensys Software Inc., 2022
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

package tech.pegasys.teku.networking.eth2.gossip;

import tech.pegasys.teku.infrastructure.async.AsyncRunner;
import tech.pegasys.teku.networking.eth2.gossip.encoding.GossipEncoding;
import tech.pegasys.teku.networking.eth2.gossip.topics.GossipTopicName;
import tech.pegasys.teku.networking.eth2.gossip.topics.OperationProcessor;
import tech.pegasys.teku.networking.p2p.gossip.GossipNetwork;
import tech.pegasys.teku.spec.config.NetworkingSpecConfig;
import tech.pegasys.teku.spec.datastructures.operations.ProposerSlashing;
import tech.pegasys.teku.spec.datastructures.state.ForkInfo;
import tech.pegasys.teku.statetransition.util.P2PDebugDataDumper;
import tech.pegasys.teku.storage.client.RecentChainData;

public class ProposerSlashingGossipManager extends AbstractGossipManager<ProposerSlashing> {

  public ProposerSlashingGossipManager(
      final RecentChainData recentChainData,
      final AsyncRunner asyncRunner,
      final GossipNetwork gossipNetwork,
      final GossipEncoding gossipEncoding,
      final ForkInfo forkInfo,
      final OperationProcessor<ProposerSlashing> processor,
      final NetworkingSpecConfig networkingConfig,
      final P2PDebugDataDumper p2pDebugDataDumper) {
    super(
        recentChainData,
        GossipTopicName.PROPOSER_SLASHING,
        asyncRunner,
        gossipNetwork,
        gossipEncoding,
        forkInfo,
        processor,
        ProposerSlashing.SSZ_SCHEMA,
        message ->
            recentChainData
                .getSpec()
                .computeEpochAtSlot(message.getHeader1().getMessage().getSlot()),
        networkingConfig,
        p2pDebugDataDumper);
  }

  public void publishProposerSlashing(final ProposerSlashing message) {
    publishMessage(message);
  }
}
