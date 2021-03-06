/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.arangodb.entity.ServerRole;
import com.arangodb.entity.ViewEntity;
import com.arangodb.entity.ViewType;

/**
 * @author Mark Vollmary
 *
 */

public class ArangoViewTest extends BaseTest {

	private static final String VIEW_NAME = "view_test";

	@BeforeClass
	public static void setup() throws InterruptedException, ExecutionException {
		if (!requireVersion(arangoDB, 3, 4)) {
			return;
		}
		db.createView(VIEW_NAME, ViewType.ARANGO_SEARCH).get();
	}

	@Test
	public void exists() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		assertThat(db.view(VIEW_NAME).exists().get(), is(true));
	}

	@Test
	public void getInfo() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		final ViewEntity info = db.view(VIEW_NAME).getInfo().get();
		assertThat(info, is(not(nullValue())));
		assertThat(info.getId(), is(not(nullValue())));
		assertThat(info.getName(), is(VIEW_NAME));
		assertThat(info.getType(), is(ViewType.ARANGO_SEARCH));
	}

	@Test
	public void drop() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		final String name = VIEW_NAME + "_droptest";
		db.createView(name, ViewType.ARANGO_SEARCH).get();
		final ArangoViewAsync view = db.view(name);
		view.drop().get();
		assertThat(view.exists().get(), is(false));
	}

	@Test
	public void rename() throws InterruptedException, ExecutionException {
		if (arangoDB.getRole().get() != ServerRole.SINGLE) {
			return;
		}
		if (!requireVersion(3, 4)) {
			return;
		}
		final String name = VIEW_NAME + "_renametest";
		final String newName = name + "_new";
		db.createView(name, ViewType.ARANGO_SEARCH).get();
		db.view(name).rename(newName).get();
		assertThat(db.view(name).exists().get(), is(false));
		assertThat(db.view(newName).exists().get(), is(true));
	}

}
