package com.myenterpriseos.myenterpriseopensource.services;

import com.myenterpriseos.myenterpriseopensource.dto.ProductRequest;
import com.myenterpriseos.myenterpriseopensource.dto.ProductResponse;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.entity.Product;
import com.myenterpriseos.myenterpriseopensource.exception.CompanyNotFoundException;
import com.myenterpriseos.myenterpriseopensource.exception.ProductInUseException;
import com.myenterpriseos.myenterpriseopensource.exception.ProductNotFoundException;
import com.myenterpriseos.myenterpriseopensource.mapper.ProductMapper;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import com.myenterpriseos.myenterpriseopensource.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;

    public ProductService(
            ProductRepository productRepository,
            CompanyRepository companyRepository) {
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public ProductResponse createProduct(
            Long companyId,
            ProductRequest productRequest) {

        Company company = findActiveCompany(companyId);

        Product product = ProductMapper.toEntity(
                productRequest,
                company);

        Product savedProduct = productRepository.save(product);

        return ProductMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse findProductById(
            Long companyId,
            Long productId) {

        findActiveCompany(companyId);

        Product product = productRepository
                .findByIdAndCompany_Id(productId, companyId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return ProductMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAllProducts(Long companyId) {

        findActiveCompany(companyId);

        return productRepository
                .findAllByCompany_Id(companyId)
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(
            Long companyId,
            Long productId,
            ProductRequest productRequest) {

        findActiveCompany(companyId);

        Product product = productRepository
                .findByIdAndCompany_Id(productId, companyId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.setName(productRequest.name());
        product.setSku(productRequest.sku());
        product.setPrice(productRequest.price());
        product.setCurrency(productRequest.currency());

        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse deleteProduct(
            Long companyId,
            Long productId) {

        findActiveCompany(companyId);

        Product product = productRepository
                .findByIdAndCompany_Id(productId, companyId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        ProductResponse response = ProductMapper.toResponse(product);

        try {
            productRepository.delete(product);
            // Execute the DELETE here so foreign-key failures are translated before commit.
            productRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ProductInUseException(productId, exception);
        }

        return response;
    }

    private Company findActiveCompany(Long companyId) {
        return companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
    }
}
