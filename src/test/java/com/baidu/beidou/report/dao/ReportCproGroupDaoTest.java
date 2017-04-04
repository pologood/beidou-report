/**
 * 
 */
package com.baidu.beidou.report.dao;

import static org.junit.Assert.*;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;

/**
 * @author Zhu Zhenxing
 * 
 */
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
@TransactionConfiguration(transactionManager = "addbTransactionManager")
public class ReportCproGroupDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

    public void setDataSource(@Qualifier("addbMultiDataSource") DataSource dataSource) {
        super.setDataSource(dataSource);
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
//        fail("Not yet implemented");
    }

}
