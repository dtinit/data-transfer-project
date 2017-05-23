import { TestBed, inject } from '@angular/core/testing';

import { SelectDataTypeService } from './select-data-type.service';

describe('SelectDataTypeService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SelectDataTypeService]
    });
  });

  it('should be created', inject([SelectDataTypeService], (service: SelectDataTypeService) => {
    expect(service).toBeTruthy();
  }));
});
