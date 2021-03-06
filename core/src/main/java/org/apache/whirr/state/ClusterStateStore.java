/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.whirr.state;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterSpec;
import org.jclouds.domain.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for cluster state storage facilities.
 * 
 */
public abstract class ClusterStateStore {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterStateStore.class);

  /**
   * Deserializes cluster state from storage.
   * 
   * @return
   * @throws IOException
   */
  public abstract Cluster load() throws IOException;

  /**
   * Try to load the cluster state or return an empty instance
   *
   * @return
   */
  public Cluster tryLoadOrEmpty() {
    try {
      return load();

    } catch (Exception e) {
      LOG.info("Unable to load cluster state, assuming it has no running nodes.", e);
      return Cluster.empty();
    }
  }

  /**
   * Saves cluster state to storage.
   * 
   * @param cluster
   * @throws IOException
   */
  public abstract void save(Cluster cluster) throws IOException;

  /**
   * Destroys the provided cluster's state in storage.
   * 
   * @throws IOException
   */
  public abstract void destroy() throws IOException;


  /**
   * Create parser friendly string representation for a {@link Cluster}
   *
   * @param cluster
   * @return String representation
   * @throws IOException
   */
  protected String serialize(Cluster cluster) throws IOException {
    StringBuilder sb = new StringBuilder();

    for (Cluster.Instance instance : cluster.getInstances()) {
      String id = instance.getId();
      String roles = Joiner.on(',').join(instance.getRoles());

      sb.append(id).append("\t");
      sb.append(roles).append("\t");
      sb.append(instance.getPublicIp()).append("\t");
      sb.append(instance.getPrivateIp()).append("\n");
    }

    return sb.toString();
  }

  /**
   * Rebuild the {@link Cluster} instance by using the string representation
   *
   * @param spec
   * @param content
   * @return
   * @throws UnknownHostException
   */
  protected Cluster unserialize(ClusterSpec spec, String content) throws UnknownHostException {
    Credentials credentials = new Credentials(spec.getClusterUser(), spec.getPrivateKey());
    Set<Cluster.Instance> instances = Sets.newLinkedHashSet();

    for(String line : Splitter.on("\n").split(content)) {
      if (line.trim().equals("")) continue; /* ignore empty lines */
      Iterator<String> fields = Splitter.on("\t").split(line).iterator();

      String id = fields.next();
      Set<String> roles = Sets.newLinkedHashSet(Splitter.on(",").split(fields.next()));
      String publicIPAddress = fields.next();
      String privateIPAddress = fields.next();

      instances.add(new Cluster.Instance(credentials, roles,
        publicIPAddress, privateIPAddress, id, null));
    }

    return new Cluster(instances);
  }

}
