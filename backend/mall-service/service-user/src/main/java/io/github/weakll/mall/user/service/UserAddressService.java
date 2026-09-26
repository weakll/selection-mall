package io.github.weakll.mall.user.service;

import io.github.weakll.mall.model.entity.user.UserAddress;

import java.util.List;

public interface UserAddressService {
    List<UserAddress> findUserAddressList();

    UserAddress getById(Long id);

    void save(UserAddress userAddress);

    void updateById(UserAddress userAddress);

    void removeById(Long id);

    void deleteById(Long id);
}
