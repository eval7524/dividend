package com.example.dividend.persist.entity;

import com.example.dividend.model.Dividend;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.SQLInsert;

import java.time.LocalDateTime;

@Entity(name = "dividend")
@Getter
@ToString
@NoArgsConstructor
@Table(name = "dividend", uniqueConstraints = @UniqueConstraint(columnNames = {"companyId", "date"}))
public class DividendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private String dividend;

    public DividendEntity(Long companyId, Dividend dividend) {
        this.companyId = companyId;
        this.date = dividend.getDate();
        this.dividend = dividend.getDividend();
    }
}
