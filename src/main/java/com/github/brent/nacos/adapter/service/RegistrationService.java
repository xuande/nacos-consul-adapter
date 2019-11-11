/**
 * The MIT License
 * Copyright (c) 2019 Brent
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.brent.nacos.adapter.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import com.github.brent.nacos.adapter.data.ChangeItem;

import rx.Single;

/**
 * Returns Services and List of Service with its last changed
 */
@Component
public class RegistrationService {

	private static final String[] NO_SERVICE_TAGS = new String[0];

	@Autowired
	private DiscoveryClient discoveryClient;

	public Single<ChangeItem<Map<String, String[]>>> getServiceNames(long waitMillis, Long index) {
		return returnDeferred(waitMillis, index, () -> {
			List<String> services = discoveryClient.getServices();
			Set<String> set = new HashSet<String>();
			set.addAll(services);

			Map<String, String[]> result = new HashMap<String, String[]>();
			for (String item : set) {
				result.put(item, NO_SERVICE_TAGS);
			}
			return result;
		});
	}

	public Single<ChangeItem<List<Map<String, Object>>>> getService(String appName, long waitMillis, Long index) {
		return returnDeferred(waitMillis, index, () -> {
			List<ServiceInstance> instances = discoveryClient.getInstances(appName);
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

			if (instances == null) {
				return Collections.emptyList();
			} else {
				Set<ServiceInstance> instSet = new HashSet<ServiceInstance>(instances);
				for (ServiceInstance instance : instSet) {
					Map<String, Object> ipObj = new HashMap<String, Object>();

					ipObj.put("Address", instance.getHost());
					ipObj.put("Node", instance.getServiceId());
					ipObj.put("ServiceAddress", instance.getHost());
					ipObj.put("ServiceName", instance.getServiceId());
					ipObj.put("ServiceID", instance.getHost() + ":" + instance.getPort());
					ipObj.put("ServicePort", instance.getPort());
					ipObj.put("NodeMeta", Collections.emptyMap());
					Map<String, String> metaJo = new HashMap<String, String>();
					metaJo.put("management.port", "" + instance.getPort());
					ipObj.put("ServiceMeta", metaJo);
					ipObj.put("ServiceTags", Collections.emptyList());

					list.add(ipObj);
				}
				return list;
			}
		});
	}

	private <T> Single<ChangeItem<T>> returnDeferred(long waitMillis, Long index, Supplier<T> fn) {
		return Single.just(new ChangeItem<>(fn.get(), new Date().getTime()));
	}
}
