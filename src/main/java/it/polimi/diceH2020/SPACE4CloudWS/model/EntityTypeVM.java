/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Jacopo Rigoli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.SPACE4CloudWS.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@IdClass(EntityKey.class)
@Table(name = "TYPEVM")
@Data
@NoArgsConstructor
public class EntityTypeVM {

    @Id
    private String type;

    @Id
    @ManyToOne
    @JoinColumn(name = "pId")
    private EntityProvider provider;

    private double cores;
    private double memory;

    private double deltaBar;
    private double rhoBar;
    private double sigmaBar;

    public EntityTypeVM(String type) {
        super();
        this.type = type;
    }
}