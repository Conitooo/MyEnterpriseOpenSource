package com.myenterpriseos.myenterpriseopensource.mapper;

import com.myenterpriseos.myenterpriseopensource.dto.ProductRequest;
import com.myenterpriseos.myenterpriseopensource.dto.ProductResponse;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.entity.Product;

public class ProductMapper {
    public static Product toEntity(ProductRequest request, Company company) {
        Product product = new Product();

        product.setName(request.name());
        product.setPrice(request.price());
        product.setCurrency(request.currency());
        product.setSku(request.sku());
        product.setCompany(company);

        return product;
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getCurrency(),
                product.getCompany().getName()
        );
    }

}
