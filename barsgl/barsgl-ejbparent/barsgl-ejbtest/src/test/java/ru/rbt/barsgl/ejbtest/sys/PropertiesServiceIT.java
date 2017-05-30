package ru.rbt.barsgl.ejbtest.sys;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejbcore.CacheController;
import ru.rbt.barsgl.ejbtest.AbstractRemoteIT;
import ru.rbt.ejb.conf.map.AbstractConfigProperty;
import ru.rbt.ejb.conf.map.ConfigNode;
import ru.rbt.ejb.conf.map.StringProperty;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.util.StringUtils;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 */
public class PropertiesServiceIT extends AbstractRemoteIT {

    private static final String prefix = "TST";
    private static final String PROPERTY_NAME = prefix + StringUtils.rsubstr(System.currentTimeMillis() + "1", 5);
    private static final String PROPERTY_NAME2 = prefix + StringUtils.rsubstr(System.currentTimeMillis() + "2", 5);
    private static final String nodeName = prefix + StringUtils.rsubstr(System.currentTimeMillis() + "", 5);

    @Test
    public void test() {

        ConfigNode node = createNode();
        StringProperty stringProperty = createProperty(node, PROPERTY_NAME);
        Assert.assertTrue(null != stringProperty);

        StringProperty property = remoteAccess.invoke(PropertiesRepository.class, "getProperty", PROPERTY_NAME);
        StringProperty propertyCached = remoteAccess.invoke(PropertiesRepository.class, "getCachedProperty", PROPERTY_NAME);
        Assert.assertEquals(property.getValue(), propertyCached.getValue());

        // изменяем свойство напрямую, в кэше старое значение
        stringProperty.setValue(StringUtils.rsubstr(System.currentTimeMillis() + "99", 5));
        baseEntityRepository.update(stringProperty);
        property = remoteAccess.invoke(PropertiesRepository.class, "getProperty", PROPERTY_NAME);
        propertyCached = remoteAccess.invoke(PropertiesRepository.class, "getCachedProperty", PROPERTY_NAME);
        Assert.assertNotEquals(property.getValue(), propertyCached.getValue());

        // сбрасываем кэш
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
        property = remoteAccess.invoke(PropertiesRepository.class, "getProperty", PROPERTY_NAME);
        propertyCached = remoteAccess.invoke(PropertiesRepository.class, "getCachedProperty", PROPERTY_NAME);
        Assert.assertEquals(property.getValue(), propertyCached.getValue());

        // пустое свойство
        baseEntityRepository.executeUpdate("delete from StringProperty p where p.id =?1", PROPERTY_NAME);
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
        Assert.assertNull(remoteAccess.invoke(PropertiesRepository.class, "getCachedProperty", PROPERTY_NAME));
        baseEntityRepository.save(stringProperty);
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
        propertyCached = remoteAccess.invoke(PropertiesRepository.class, "getCachedProperty", PROPERTY_NAME);
        Assert.assertEquals(property.getValue(), propertyCached.getValue());
    }

    @Test
    public void testGetAllObjects() {
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");

        ConfigNode node = createNode();
        StringProperty stringProperty1 = createProperty(node,PROPERTY_NAME);
        StringProperty stringProperty2 = createProperty(node,PROPERTY_NAME2);
        Assert.assertNotNull(stringProperty1);
        Assert.assertNotNull(stringProperty2);

        List<AbstractConfigProperty> list = remoteAccess.invoke(PropertiesRepository.class, "getAllObjectsCached");
        Assert.assertTrue(2 < list.size());
        Assert.assertTrue(checkPropertyExists(list, stringProperty1));
        Assert.assertTrue(checkPropertyExists(list, stringProperty2));

        baseEntityRepository.executeUpdate("delete from StringProperty p where p.id = ?1", stringProperty2.getId());
        list = remoteAccess.invoke(PropertiesRepository.class, "getAllObjectsCached");
        Assert.assertTrue(checkPropertyExists(list, stringProperty1));
        Assert.assertTrue(checkPropertyExists(list, stringProperty2));

        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
        list = remoteAccess.invoke(PropertiesRepository.class, "getAllObjectsCached");
        Assert.assertTrue(checkPropertyExists(list, stringProperty1));
        Assert.assertFalse(checkPropertyExists(list, stringProperty2));

        baseEntityRepository.executeUpdate("delete from StringProperty p where p.id = ?1", stringProperty1.getId());
        remoteAccess.invoke(CacheController.class, "flushAllCaches");
        list = remoteAccess.invoke(PropertiesRepository.class, "getAllObjectsCached");
        Assert.assertFalse(checkPropertyExists(list, stringProperty1));
        Assert.assertFalse(checkPropertyExists(list, stringProperty2));
    }

    @Before
    public void before() {
        clean();
    }

    @AfterClass
    public static void tearDown() {
        clean();
    }

    private ConfigNode createNode() {
        ConfigNode node = new ConfigNode();
        node.setNodeName(nodeName);
        node.setDescription("desc node");
        return  (ConfigNode) baseEntityRepository.save(node);
    }

    private StringProperty createProperty(ConfigNode node, String propertyName) {
        StringProperty stringProperty = new StringProperty();
        stringProperty.setId(propertyName);
        stringProperty.setNode(node);
        stringProperty.setRequired(true);
        stringProperty.setValue(StringUtils.rsubstr(System.currentTimeMillis() + "", 5));
        stringProperty.setDescription("desc prop");
        return  (StringProperty) baseEntityRepository.save(stringProperty);
    }

    @Test public void test1() {
        remoteAccess.invoke(CacheController.class, "flushAllCaches");
    }

    private static void clean() {
        baseEntityRepository.executeUpdate("delete from StringProperty p where p.id =?1", PROPERTY_NAME);
        baseEntityRepository.executeUpdate("delete from StringProperty p where p.id =?1", PROPERTY_NAME2);
        baseEntityRepository.executeUpdate("delete from ConfigNode p where p.nodeName =?1", nodeName);
    }

    private boolean checkPropertyExists(List<AbstractConfigProperty> list, StringProperty exists) {
        return 1 == list.stream().filter(new Predicate<AbstractConfigProperty>() {
            @Override
            public boolean test(AbstractConfigProperty property) {
                return property.equals(exists);
            }
        }).count();
    }

}
