import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  activeStep = signal<number | null>(null);

  toggleStep(step: number): void {
    this.activeStep.update(current => current === step ? null : step);
  }
}