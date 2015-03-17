/*
 * Copyright 2008,  Unitils.org
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
 */
package com.sunlights.common;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.unitils.UnitilsJUnit4;
import org.unitils.dbunit.annotation.DataSet;
import org.unitils.dbunit.annotation.ExpectedDataSet;
import org.unitils.orm.jpa.JpaUnitils;
import org.unitils.orm.jpa.annotation.JpaEntityManagerFactory;
import org.unitils.reflectionassert.ReflectionAssert;
import play.db.jpa.JPA;
import play.test.FakeApplication;
import play.test.Helpers;

public class HibernateJpaTest extends UnitilsJUnit4 {

	@JpaEntityManagerFactory(persistenceUnit = "test", configFile = "com/sunlights/commmon/hibernate-persistence-test.xml")
	EntityManagerFactory entityManagerFactory;
	
	@PersistenceContext
	EntityManager entityManager;

    protected FakeApplication app;

    protected FakeApplication provideFakeApplication() {
        return Helpers.fakeApplication();
    }

    @Before
    public void startPlay() {
        Helpers.start(this.provideFakeApplication());
        JPA.bindForCurrentThread(entityManager);
    }

    @After
    public void stopPlay() {
        if(this.app != null) {
            Helpers.stop(this.app);
            this.app = null;
        }

    }

//    @Test
//    @DataSet("../datasets/SinglePerson.xml")
//    public void testFindById() {
//        Person userFromDb = entityManager.find(Person.class, 1L);
//        ReflectionAssert.assertLenientEquals(person, userFromDb);
//    }
//
//    @Test
//    @DataSet("../datasets/NoPersons.xml")
//    @ExpectedDataSet("../datasets/SinglePerson-result.xml")
//    public void testPersist() {
//        entityManager.persist(person);
//    }
//
//    @Test
//    public void testMapping() {
//        JpaUnitils.assertMappingWithDatabaseConsistent();
//    }

}