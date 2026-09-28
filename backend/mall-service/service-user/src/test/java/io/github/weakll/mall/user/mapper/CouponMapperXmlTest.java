package io.github.weakll.mall.user.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CouponMapperXmlTest {

    @Test
    void registersEveryCouponInfoMapperStatement() throws IOException {
        Configuration configuration = loadMapper("mapper/user/CouponInfoMapper.xml");

        assertAll(List.of("findAllPublished", "getById", "getByIdForUpdate", "save", "updateReceiveCount")
                .stream()
                .map(statement -> () -> assertTrue(configuration.hasStatement(
                        "io.github.weakll.mall.user.mapper.CouponInfoMapper." + statement))));
    }

    @Test
    void registersEveryCouponUserMapperStatement() throws IOException {
        Configuration configuration = loadMapper("mapper/user/CouponUserMapper.xml");

        assertAll(List.of("findByUserId", "findByUserIdAndCouponId", "countByUserIdAndCouponId", "save", "updateUsed")
                .stream()
                .map(statement -> () -> assertTrue(configuration.hasStatement(
                        "io.github.weakll.mall.user.mapper.CouponUserMapper." + statement))));
    }

    private Configuration loadMapper(String resource) throws IOException {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
}
