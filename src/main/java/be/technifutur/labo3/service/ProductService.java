package be.technifutur.labo3.service;

import be.technifutur.labo3.dto.ProductDTO;
import be.technifutur.labo3.entity.Product;
import be.technifutur.labo3.entity.QProduct;
import be.technifutur.labo3.entity.Supplier;
import be.technifutur.labo3.mapper.Mapper;
import be.technifutur.labo3.repository.ProductRepository;
import be.technifutur.labo3.repository.SupplierRepository;
import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ProductService implements Crudable<Product, ProductDTO, Integer> {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final Mapper mapper;

    public ProductService(ProductRepository productRepository, SupplierRepository supplierRepository, Mapper mapper) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.mapper = mapper;
    }

    @Override
    public List<ProductDTO> getAll() {
        return productRepository.findAll()
                .stream()
                .map(mapper::toProductDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDTO getById(Integer integer) {
        return mapper.toProductDTO(
                productRepository.findById(integer)
                        .orElseThrow(() -> new NoSuchElementException())
        );
    }

    @Override
    public boolean insert(Product product) {
        Product p = productRepository.save(product);
        return productRepository.existsById(p.getId());
    }


    @Override
    public boolean update(Product product, Integer integer) {
        Product old = productRepository.getOne(integer);
        Product toTest = new Product(
                old.getId(),
                old.getName(),
                old.getDescription(),
                old.getInsertDate(),
                old.getUpdateDate(),
                old.getExpirationDate(),
                old.getPrice(),
                old.getQuantity(),
                old.getImagePath(),
                old.getTva(),
                old.getCategories(),
                old.getSupplier(),
                old.isInactive()
        );
        product.setId(integer);
        product.setInsertDate(toTest.getInsertDate());
        productRepository.save(product);
        return !toTest.equals(productRepository.getOne(integer));
    }

    @Override
    public boolean delete(Integer integer) {
        Product toDelete = productRepository.getOne(integer);
        toDelete.setInactive(true);
        productRepository.save(toDelete);
        Product product = productRepository.getOne(integer);
        System.out.println("TEST" + product.isInactive());
        return product.isInactive();
    }

    public Page<ProductDTO> searchByProductName(String productName, int page, int size, String sortingFieldName, String sortingDirection){

        BooleanBuilder predicate = new BooleanBuilder();

        QProduct qProduct = QProduct.product;

        if(productName != null && !productName.equals("")){
            predicate.and(qProduct.name.containsIgnoreCase(productName));
        }

        Pageable pageable = null;
        if (sortingFieldName.equalsIgnoreCase("") && sortingDirection.equalsIgnoreCase("")){
            pageable= PageRequest.of(page,size);
        } else{
            if (sortingDirection.equalsIgnoreCase("asc")){
                pageable=PageRequest.of(page,size, Sort.by(sortingFieldName));
            } else if (sortingDirection.equalsIgnoreCase("desc")){
                pageable=PageRequest.of(page,size, Sort.by(sortingFieldName).descending());
            }
        }

        List<ProductDTO> result = this.productRepository.findAll(predicate,pageable)
                .stream()
                .map(mapper::toProductDTO)
                .filter(p -> !p.isInactive())
                .collect(Collectors.toList());
        long total = StreamSupport.stream(this.productRepository.findAll(predicate).spliterator(),false)
                .collect(Collectors.toList())
                .stream()
                .filter(p -> !p.isInactive())
                .count();
        return new PageImpl<>(result, PageRequest.of(page, size), total);
    }

    public Page<ProductDTO> searchByProduct(Product product, int page, int size, String sortingFieldName, String sortingDirection){
        BooleanBuilder predicate = new BooleanBuilder();

        QProduct qProduct = QProduct.product;

        if(product.getName() != null && !product.getName().equals("")){
            predicate.and(qProduct.name.containsIgnoreCase(product.getName()));
        }

        if(product.getDescription() != null && !product.getDescription().equals("")){
            predicate.and(qProduct.description.containsIgnoreCase((product.getDescription())));
        }

        // Les produits recherch??s auront une date d'expiration post??rieure ?? celle recherch??e
        if (product.getExpirationDate() != null){
            predicate.and(qProduct.expirationDate.after(product.getExpirationDate()));
        }

        // Les produits recherch??s auront un prix inf??rieur ou ??gal ?? celui recherch??
        if (product.getPrice() > 0){
            predicate.and(qProduct.price.loe(product.getPrice()));
        }

        // Les produits recherch??s auront une quantit?? en stock sup??rieure ou ??gale ?? celle recherch??e
        if(product.getQuantity() != null && product.getQuantity() >0){
            predicate.and(qProduct.quantity.goe((product.getQuantity())));
        }

        Pageable pageable = null;
        if (sortingFieldName.equalsIgnoreCase("") && sortingDirection.equalsIgnoreCase("")){
            pageable= PageRequest.of(page,size);
        } else{
            if (sortingDirection.equalsIgnoreCase("asc")){
                pageable=PageRequest.of(page,size, Sort.by(sortingFieldName));
            } else if (sortingDirection.equalsIgnoreCase("desc")){
                pageable=PageRequest.of(page,size, Sort.by(sortingFieldName).descending());
            }
        }

        List<ProductDTO> result = this.productRepository.findAll(predicate,pageable)
                .stream()
                .map(mapper::toProductDTO)
                .filter(p -> !p.isInactive())
                .collect(Collectors.toList());
        long total = StreamSupport.stream(this.productRepository.findAll(predicate).spliterator(),false)
                .collect(Collectors.toList())
                .stream()
                .filter(p -> !p.isInactive())
                .count();
        return new PageImpl<>(result, PageRequest.of(page, size), total);
    }

    public Page<ProductDTO> getAllWithPagination(int page, int size, String sortingFieldName, String sortingDirection){

//        long total = this.productRepository.findAll().stream().count();
        long total = this.productRepository.findByInactiveFalse().size();

        Pageable pageable = null;
        if (sortingFieldName.equalsIgnoreCase("") && sortingDirection.equalsIgnoreCase("")){
            pageable= PageRequest.of(page,size);
        } else{
            if (sortingDirection.equalsIgnoreCase("asc")){
                pageable=PageRequest.of(page,size, Sort.by(sortingFieldName));
            } else if (sortingDirection.equalsIgnoreCase("desc")){
                pageable=PageRequest.of(page,size, Sort.by(sortingFieldName).descending());
            }
        }


//        List<ProductDTO> result = this.productRepository.findAll(PageRequest.of(page, size))
        List<ProductDTO> result = this.productRepository.findByInactiveFalse(pageable)
                .stream()
                .map(mapper::toProductDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(result, PageRequest.of(page, size), total);
    }
}
