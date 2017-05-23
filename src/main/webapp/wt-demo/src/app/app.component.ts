import { Component, Input } from '@angular/core';
import { ServiceDescription, ServiceDescriptions } from './service-description';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'World Takeout Demo App';
  emptyServices : ServiceDescription[] = [];
  currentServiceDescriptions: ServiceDescriptions = new ServiceDescriptions(this.emptyServices, this.emptyServices);

  // Listens to event setting the serviceDescriptions in child components.
  onDataTypeSelection(serviceDescriptions) {
    console.log("Updating currentServiceDescriptions: "
      + ', importServices: ' + JSON.stringify(serviceDescriptions.importServices)
      + ', exportServices: ' + JSON.stringify(serviceDescriptions.exportServices));
    this.currentServiceDescriptions = serviceDescriptions;
  }
}
