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

package tech.pegasys.teku.networking.p2p.libp2p.gossip;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import tech.pegasys.teku.networking.p2p.gossip.TopicHandler;

class GossipTopicHandlers {

  private final Map<String, TopicHandler> topicToHandlerMap = new ConcurrentHashMap<>();

  public void add(String topic, TopicHandler handler) {
    topicToHandlerMap.put(topic, handler);
  }

  public Optional<TopicHandler> getHandlerForTopic(String topic) {
    return Optional.ofNullable(topicToHandlerMap.get(topic));
  }
}
