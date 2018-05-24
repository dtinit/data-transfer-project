import {Injectable} from "@angular/core";
import {SystemEvent} from "./system.event";
import {Arrays} from "../util";

/**
 * Broadcasts events to topics.
 */
@Injectable()
export class EventService {
    private subjects = new Map<string, Array<(event: SystemEvent) => void>>();

    /**
     * Subscribes to a topic.
     * @param topic the topic
     * @param observer
     * @returns the closable
     */
    subscribe(topic: string, observer: (value: SystemEvent) => void): () => void {
        let subject = this.subjects.get(topic);
        if (subject === undefined) {
            subject = [];
            this.subjects.set(topic, subject);
        }

        subject.push(observer);
        return () => {
            let subject = this.subjects.get(topic);
            if (subject != null) {
                Arrays.removeElement(observer, subject);
                if (subject.length === 0) {
                    this.subjects.delete(topic);
                }
            }
        };
    }

    /**
     * Dispatches an event to the topic.
     * @param topic the topic
     * @param event the event
     */
    publish(topic: string, event: SystemEvent) {
        let subject = this.subjects.get(topic);
        if (subject === undefined) {
            return;
        }
        for (let observer of subject) {
            observer(event);
        }
    }


}


