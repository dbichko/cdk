/* $RCSfile$
 * $Author$    
 * $Date$    
 * $Revision$
 * 
 * Copyright (C) 1997-2007  The Chemistry Development Kit (CDK) project
 * 
 * Contact: cdk-devel@lists.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA. 
 * 
 */
package org.openscience.cdk.silent;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.ISingleElectron;
import org.openscience.cdk.interfaces.AbstractSingleElectronTest;
import org.openscience.cdk.interfaces.ITestObjectBuilder;

/**
 * Checks the functionality of the {@link SingleElectron}.
 *
 * @cdk.module test-silent
 */
public class SingleElectronTest extends AbstractSingleElectronTest {

    @BeforeClass public static void setUp() {
        setTestObjectBuilder(new ITestObjectBuilder() {
            public IChemObject newTestObject() {
                return new SingleElectron();
            }
        });
    }

    @Test public void testSingleElectron() {
        ISingleElectron radical = new SingleElectron();
        Assert.assertNull(radical.getAtom());
        Assert.assertEquals(1, radical.getElectronCount().intValue());
    }
    
    @Test public void testSingleElectron_IAtom() {
        IAtom atom = newChemObject().getBuilder().newInstance(IAtom.class,"N");
        ISingleElectron radical = new SingleElectron(atom);
        Assert.assertEquals(1, radical.getElectronCount().intValue());
        Assert.assertEquals(atom, radical.getAtom());
        Assert.assertTrue(radical.contains(atom));
    }

    // Overwrite default methods: no notifications are expected!
    
    @Test public void testNotifyChanged() {
        ChemObjectTestHelper.testNotifyChanged(newChemObject());
    }
    @Test public void testNotifyChanged_SetFlag() {
        ChemObjectTestHelper.testNotifyChanged_SetFlag(newChemObject());
    }
    @Test public void testNotifyChanged_SetFlags() {
        ChemObjectTestHelper.testNotifyChanged_SetFlags(newChemObject());
    }
    @Test public void testNotifyChanged_IChemObjectChangeEvent() {
        ChemObjectTestHelper.testNotifyChanged_IChemObjectChangeEvent(newChemObject());
    }
    @Test public void testStateChanged_IChemObjectChangeEvent() {
        ChemObjectTestHelper.testStateChanged_IChemObjectChangeEvent(newChemObject());
    }
    @Test public void testClone_ChemObjectListeners() throws Exception {
        ChemObjectTestHelper.testClone_ChemObjectListeners(newChemObject());
    }
    @Test public void testAddListener_IChemObjectListener() {
        ChemObjectTestHelper.testAddListener_IChemObjectListener(newChemObject());
    }
    @Test public void testGetListenerCount() {
        ChemObjectTestHelper.testGetListenerCount(newChemObject());
    }
    @Test public void testRemoveListener_IChemObjectListener() {
        ChemObjectTestHelper.testRemoveListener_IChemObjectListener(newChemObject());
    }
    @Test public void testSetNotification_true() {
        ChemObjectTestHelper.testSetNotification_true(newChemObject());
    }
    @Test public void testNotifyChanged_SetProperty() {
        ChemObjectTestHelper.testNotifyChanged_SetProperty(newChemObject());
    }
    @Test public void testNotifyChanged_RemoveProperty() {
        ChemObjectTestHelper.testNotifyChanged_RemoveProperty(newChemObject());
    }
}
